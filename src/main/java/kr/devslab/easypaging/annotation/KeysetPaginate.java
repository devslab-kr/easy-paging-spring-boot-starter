package kr.devslab.easypaging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a controller method as keyset (cursor)-paginated.
 *
 * <p>Unlike {@link AutoPaginate}, this annotation drives an
 * {@code ArgumentResolver} — it does NOT wrap the result. The handler is
 * expected to consume a
 * {@link kr.devslab.easypaging.core.KeysetRequest} and return a
 * {@link kr.devslab.easypaging.core.KeysetPage}.
 *
 * <p>Use this for unbounded or very large tables (time-series, logs, location
 * tracks) where {@code COUNT(*)} and {@code OFFSET} both become bottlenecks.
 *
 * <pre>{@code
 * @GetMapping("/locations")
 * @KeysetPaginate(keys = {"time", "id"}, direction = "DESC", defaultSize = 50)
 * public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID worker) {
 *     List<Location> rows = mapper.findAfter(
 *         worker,
 *         req.keyAsInstant("time"),
 *         req.keyAsLong("id"),
 *         req.size() + 1);                 // +1 row to detect whether a next page exists
 *     return KeysetPage.build(rows, req, r -> Map.of(
 *         "time", r.getTime(),
 *         "id",   r.getId()));
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeysetPaginate {

    /**
     * The ordered list of key columns. Order matters — the cursor is decoded
     * in the same order. Typical pattern: timestamp followed by an id
     * tiebreaker to guarantee a deterministic order.
     */
    String[] keys();

    /**
     * Default sort direction applied to all keys. Per-key directions are not
     * supported by this annotation; declare a stable composite direction
     * (e.g. all {@code DESC}) or build the {@code ORDER BY} manually in the
     * mapper.
     */
    String direction() default "DESC";

    /** Page size when caller omits {@code size} query parameter. */
    int defaultSize() default 20;

    /** Hard upper bound on page size. */
    int maxSize() default 100;
}
