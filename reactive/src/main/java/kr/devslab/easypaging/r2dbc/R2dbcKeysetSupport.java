package kr.devslab.easypaging.r2dbc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import kr.devslab.easypaging.core.Cursor;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

/**
 * Keyset (cursor) pagination helper for Spring Data R2DBC.
 *
 * <p>Mirrors the MyBatis-side {@code KeysetPage.build} flow on a reactive
 * stack:
 *
 * <ol>
 *   <li>Derive the lexicographic {@code WHERE} clause from the cursor in the
 *       {@link KeysetRequest} (an empty cursor → just the {@code baseFilter}).</li>
 *   <li>Derive the {@code ORDER BY} — natural direction for {@code FORWARD}
 *       scans, flipped for {@code BACKWARD} so the closest-to-cursor rows
 *       arrive first.</li>
 *   <li>Fetch {@code size + 1} rows; {@link KeysetPage#build} trims, reverses
 *       on {@code BACKWARD}, and encodes the next/prev cursors.</li>
 * </ol>
 *
 * <p>Example:
 *
 * <pre>{@code
 * private static final List<KeyColumn> KEYS = List.of(
 *     new KeyColumn("created_at", "createdAt", Instant.class, SortDirection.DESC),
 *     new KeyColumn("id",         "id",        Long.class,    SortDirection.DESC));
 *
 * @GetMapping("/locations")
 * @KeysetPaginate(keys = {"createdAt", "id"}, direction = "DESC", defaultSize = 50)
 * public Mono<KeysetPage<Location>> stream(KeysetRequest request) {
 *     return R2dbcKeysetSupport.paginate(
 *         template,
 *         Location.class,
 *         Criteria.empty(),
 *         KEYS,
 *         request,
 *         loc -> Map.of("createdAt", loc.getCreatedAt(), "id", loc.getId()),
 *         codec);
 * }
 * }</pre>
 */
public final class R2dbcKeysetSupport {

    private R2dbcKeysetSupport() {}

    /**
     * Per-column metadata: the DB column name, the matching cursor key name
     * (often the same, but allowed to differ for camel/snake mismatches), the
     * Java type the column maps to (used to coerce cursor values back to the
     * right type when they come out of the JSON cursor payload as Strings or
     * Numbers), and the natural sort direction.
     *
     * <p>Built-in {@code javaType} support covers {@link Long}, {@link Integer},
     * {@link String}, {@link Instant}, {@link LocalDateTime},
     * {@link OffsetDateTime}, {@link LocalDate}, and {@link UUID}. For other
     * types, post-process the cursor value with a custom converter outside
     * this helper and call the underlying {@code R2dbcEntityTemplate} directly.
     */
    public record KeyColumn(String column, String cursorKey, Class<?> javaType,
                             SortDirection naturalDirection) {

        public KeyColumn {
            Objects.requireNonNull(column, "column");
            Objects.requireNonNull(cursorKey, "cursorKey");
            Objects.requireNonNull(javaType, "javaType");
            Objects.requireNonNull(naturalDirection, "naturalDirection");
        }

        /** Shorthand when column and cursor key are the same name. */
        public static KeyColumn of(String name, Class<?> javaType, SortDirection direction) {
            return new KeyColumn(name, name, javaType, direction);
        }
    }

    public enum SortDirection { ASC, DESC }

    public static <T> Mono<KeysetPage<T>> paginate(R2dbcEntityTemplate template,
                                                    Class<T> entityClass,
                                                    Criteria baseFilter,
                                                    List<KeyColumn> keys,
                                                    KeysetRequest request,
                                                    Function<T, Map<String, Object>> keyExtractor,
                                                    CursorCodec codec) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(entityClass, "entityClass");
        Objects.requireNonNull(baseFilter, "baseFilter (use Criteria.empty() for none)");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        Objects.requireNonNull(codec, "codec");
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("at least one key column is required");
        }

        Cursor.Direction scanDirection = request.direction();
        Criteria filter = combineWithKeysetCursor(baseFilter, keys, request, scanDirection);
        Sort sort = sortForDirection(keys, scanDirection);

        // Fetch one extra row so KeysetPage.build can detect "more rows exist".
        Query query = Query.query(filter).sort(sort).limit(request.size() + 1);

        return template.select(entityClass)
                .matching(query)
                .all()
                .collectList()
                .map(rows -> KeysetPage.build(rows, request, keyExtractor, codec));
    }

    /**
     * Builds the lexicographic-comparison WHERE clause and ANDs it with the
     * base filter. On the first page (empty cursor) only the base filter is
     * applied.
     */
    private static Criteria combineWithKeysetCursor(Criteria baseFilter,
                                                     List<KeyColumn> keys,
                                                     KeysetRequest request,
                                                     Cursor.Direction scanDirection) {
        if (request.isFirstPage()) {
            return baseFilter;
        }
        Criteria keyset = lexicographicCriteria(keys, request.keys(), scanDirection);
        return baseFilter.isEmpty() ? keyset : baseFilter.and(keyset);
    }

    /**
     * For keys {@code (k1, k2, k3)} produces:
     *
     * <pre>
     *      (k1 cmp v1)
     *   OR (k1 = v1 AND k2 cmp v2)
     *   OR (k1 = v1 AND k2 = v2 AND k3 cmp v3)
     * </pre>
     *
     * where {@code cmp} is {@code <} or {@code >} depending on each column's
     * natural direction combined with the scan direction.
     */
    private static Criteria lexicographicCriteria(List<KeyColumn> keys,
                                                   Map<String, Object> cursorValues,
                                                   Cursor.Direction scanDirection) {
        Criteria result = null;
        for (int boundary = 0; boundary < keys.size(); boundary++) {
            // Equality prefix: k1 = v1 AND ... AND k(boundary-1) = v(boundary-1)
            Criteria clause = null;
            for (int i = 0; i < boundary; i++) {
                KeyColumn k = keys.get(i);
                Object v = coerce(cursorValues.get(k.cursorKey()), k.javaType());
                Criteria eq = Criteria.where(k.column()).is(v);
                clause = (clause == null) ? eq : clause.and(eq);
            }

            // Boundary column: k(boundary) < or > v(boundary).
            KeyColumn boundaryKey = keys.get(boundary);
            Object boundaryValue = coerce(cursorValues.get(boundaryKey.cursorKey()), boundaryKey.javaType());
            Criteria comparison = strictComparison(boundaryKey, boundaryValue, scanDirection);
            clause = (clause == null) ? comparison : clause.and(comparison);

            result = (result == null) ? clause : result.or(clause);
        }
        return result;
    }

    /**
     * Cursor values come out of the JSON payload as Strings (for textual ISO
     * timestamps, UUIDs) or Numbers (for ids). R2DBC's parameter binding
     * needs the right Java type to map to the column's SQL type — feeding
     * a String into a {@code TIMESTAMP WITH TIME ZONE} column fails with
     * "Data conversion error". This coercer handles the common cases.
     */
    @SuppressWarnings("unchecked")
    private static Object coerce(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        // Numbers can come in narrower types (Integer where Long expected, etc.).
        if (value instanceof Number n) {
            if (targetType == Long.class || targetType == long.class) return n.longValue();
            if (targetType == Integer.class || targetType == int.class) return n.intValue();
            if (targetType == Double.class || targetType == double.class) return n.doubleValue();
            if (targetType == Float.class || targetType == float.class) return n.floatValue();
            if (targetType == Short.class || targetType == short.class) return n.shortValue();
        }
        String s = value.toString();
        if (targetType == String.class) return s;
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(s);
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(s);
        if (targetType == Double.class || targetType == double.class) return Double.parseDouble(s);
        if (targetType == Float.class || targetType == float.class) return Float.parseFloat(s);
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(s);
        if (targetType == Instant.class) return Instant.parse(s);
        if (targetType == LocalDateTime.class) return LocalDateTime.parse(s);
        if (targetType == OffsetDateTime.class) return OffsetDateTime.parse(s);
        if (targetType == LocalDate.class) return LocalDate.parse(s);
        if (targetType == UUID.class) return UUID.fromString(s);
        throw new IllegalArgumentException(
                "Cannot coerce cursor value '" + s + "' to " + targetType.getName() +
                "; supported types are String, primitive wrappers, Instant, LocalDateTime, " +
                "OffsetDateTime, LocalDate, UUID. For other types, build the keyset Criteria " +
                "manually instead of using R2dbcKeysetSupport.paginate(...).");
    }

    /**
     * {@code <} when scanning along the natural sort, {@code >} when scanning
     * against it (the BACKWARD case).
     *
     * <ul>
     *   <li>DESC natural + FORWARD scan → values getting smaller → {@code <}</li>
     *   <li>DESC natural + BACKWARD scan → values getting larger  → {@code >}</li>
     *   <li>ASC natural + FORWARD scan → values getting larger    → {@code >}</li>
     *   <li>ASC natural + BACKWARD scan → values getting smaller  → {@code <}</li>
     * </ul>
     */
    private static Criteria strictComparison(KeyColumn key, Object value, Cursor.Direction scanDirection) {
        boolean lessThan = (key.naturalDirection() == SortDirection.DESC && scanDirection == Cursor.Direction.FORWARD)
                        || (key.naturalDirection() == SortDirection.ASC && scanDirection == Cursor.Direction.BACKWARD);
        return lessThan
                ? Criteria.where(key.column()).lessThan(value)
                : Criteria.where(key.column()).greaterThan(value);
    }

    /**
     * Natural sort for FORWARD scans, flipped sort for BACKWARD so the rows
     * closest to the cursor arrive first. {@link KeysetPage#build} reverses
     * the BACKWARD result list back into display order before encoding cursors.
     */
    private static Sort sortForDirection(List<KeyColumn> keys, Cursor.Direction scanDirection) {
        List<Sort.Order> orders = new ArrayList<>(keys.size());
        for (KeyColumn k : keys) {
            boolean desc = (k.naturalDirection() == SortDirection.DESC && scanDirection == Cursor.Direction.FORWARD)
                        || (k.naturalDirection() == SortDirection.ASC && scanDirection == Cursor.Direction.BACKWARD);
            orders.add(desc ? Sort.Order.desc(k.column()) : Sort.Order.asc(k.column()));
        }
        return Sort.by(orders);
    }
}
