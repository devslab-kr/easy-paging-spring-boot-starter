package kr.devslab.easypaging.support;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.data.domain.Sort;

/**
 * Translates Spring Data {@link Sort} into a SQL-safe {@code ORDER BY} fragment
 * for {@code PageHelper.orderBy}.
 *
 * <p>Property names are validated against a strict whitelist
 * ({@code [A-Za-z_][A-Za-z0-9_.]*}) to prevent SQL injection through the
 * {@code sort} query parameter. A property containing anything else is dropped
 * with an {@link IllegalArgumentException}.
 */
public final class SortConverter {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_.]*");

    private SortConverter() {}

    /**
     * @return an {@code ORDER BY}-compatible fragment, or empty string if the
     *         sort is unsorted/null.
     */
    public static String toOrderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        return StreamSupport.stream(sort.spliterator(), false)
                .map(SortConverter::convertOrder)
                .collect(Collectors.joining(", "));
    }

    private static String convertOrder(Sort.Order order) {
        String property = order.getProperty();
        if (!SAFE_IDENTIFIER.matcher(property).matches()) {
            throw new IllegalArgumentException(
                    "Sort property contains illegal characters: " + property);
        }
        String direction = order.getDirection().name().toLowerCase(Locale.ROOT);
        String nullHandling = switch (order.getNullHandling()) {
            case NULLS_FIRST -> " nulls first";
            case NULLS_LAST -> " nulls last";
            case NATIVE -> "";
        };
        return property + " " + direction + nullHandling;
    }
}
