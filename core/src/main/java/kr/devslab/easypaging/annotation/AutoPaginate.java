package kr.devslab.easypaging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method (typically a Spring controller handler) as auto-paginated.
 *
 * <p>The aspect intercepts the call, reads a Spring {@code Pageable} from the
 * method arguments, configures PageHelper for the next MyBatis query, and
 * wraps the returned {@code List} into a {@link kr.devslab.easypaging.core.PageResponse}.
 *
 * <p>The aspect decides whether to wrap by inspecting the method's declared
 * return type:
 * <ul>
 *   <li>{@code PageResponse<T>}: wrap the mapper's {@code List<T>} result.</li>
 *   <li>{@code List<T>}: pass through unwrapped (still sliced and sorted by
 *       PageHelper — just no envelope).</li>
 *   <li>{@code Object}: wrap.</li>
 *   <li>Anything else: pass through.</li>
 * </ul>
 *
 * <p>Typical usage:
 * <pre>{@code
 * @GetMapping("/reports")
 * @AutoPaginate(maxSize = 50)
 * public PageResponse<Report> list(Pageable pageable) {
 *     return PageResponse.from(reportMapper.findAll(), pageable);
 *     // or simply: return reportMapper.findAll();  -- the aspect wraps it.
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoPaginate {

    /**
     * Whether to run the auxiliary {@code COUNT} query that backs
     * {@code totalElements}/{@code totalPages}.
     * <p>Disable for very large or unbounded tables (e.g. time-series logs)
     * where the count would dominate query time. Disabling means
     * {@code totalElements} and {@code totalPages} are reported as {@code -1}.
     */
    boolean count() default true;

    /**
     * Hard upper bound on page size. Caller-supplied sizes above this value
     * are clamped. Protects against malicious or accidental DoS via huge pages.
     */
    int maxSize() default 100;

    /**
     * PageHelper "reasonable" mode: when {@code true}, page numbers below 1 or
     * above the last page are silently clamped instead of returning empty.
     */
    boolean reasonable() default true;
}
