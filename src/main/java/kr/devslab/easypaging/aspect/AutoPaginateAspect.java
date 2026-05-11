package kr.devslab.easypaging.aspect;

import com.github.pagehelper.PageHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.easypaging.autoconfigure.EasyPagingProperties;
import kr.devslab.easypaging.core.PageResponse;
import kr.devslab.easypaging.spi.PageResponseFactory;
import kr.devslab.easypaging.support.SortConverter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * Synchronous aspect that converts {@link AutoPaginate}-annotated methods
 * into PageHelper-driven, offset-based paginated calls.
 *
 * <p>Lifecycle of a single invocation:
 * <ol>
 *   <li>Resolve a {@link Pageable} from the method arguments (or fall back to a
 *       sensible default from {@link EasyPagingProperties}).</li>
 *   <li>Clamp page size to the smaller of the annotation's {@code maxSize} and
 *       the global property {@code easy-paging.max-page-size}.</li>
 *   <li>Translate the pageable's {@link org.springframework.data.domain.Sort}
 *       into a SQL-safe {@code ORDER BY} via {@link SortConverter}.</li>
 *   <li>Call {@code PageHelper.startPage(...)}; the very next MyBatis query
 *       executed in this thread will be paginated.</li>
 *   <li>Invoke the method. On {@code List<?>} return, optionally wrap it via
 *       {@link PageResponseFactory} (if registered) or
 *       {@link PageResponse#from(List, Pageable)}.</li>
 *   <li>Always call {@code PageHelper.clearPage()} in {@code finally} to
 *       eliminate ThreadLocal leakage — particularly important when running on
 *       Virtual Threads where threads are short-lived but ThreadLocals still
 *       accumulate.</li>
 * </ol>
 *
 * <p>Reactive return types ({@code Mono}, {@code Flux}) are intentionally NOT
 * wrapped here — the reactive aspect handles those via Reactor Context.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AutoPaginateAspect {

    private static final Logger log = LoggerFactory.getLogger(AutoPaginateAspect.class);

    private final EasyPagingProperties properties;

    @Nullable
    private final PageResponseFactory responseFactory;

    public AutoPaginateAspect(EasyPagingProperties properties,
                               @Nullable PageResponseFactory responseFactory) {
        this.properties = properties;
        this.responseFactory = responseFactory;
    }

    @Around("@annotation(autoPaginate)")
    public Object around(ProceedingJoinPoint pjp, AutoPaginate autoPaginate) throws Throwable {
        Pageable pageable = resolvePageable(pjp.getArgs());
        int effectiveSize = clampSize(pageable.getPageSize(), autoPaginate.maxSize());
        int pageNum = Math.max(0, pageable.getPageNumber()) + 1; // PageHelper is 1-indexed

        PageHelper.startPage(
                pageNum,
                effectiveSize,
                autoPaginate.count(),
                autoPaginate.reasonable(),
                /* pageSizeZero = */ Boolean.FALSE);

        String orderBy = SortConverter.toOrderBy(pageable.getSort());
        if (!orderBy.isEmpty()) {
            PageHelper.orderBy(orderBy);
        }

        Class<?> declaredReturnType = ((MethodSignature) pjp.getSignature()).getReturnType();
        try {
            Object result = pjp.proceed();
            return adaptResult(result, pageable, declaredReturnType);
        } finally {
            // Defensive: if the method threw before the mapper ran, the page
            // remains parked on PageHelper's thread-local stack. Clear it so
            // the next request on this (virtual or platform) thread starts clean.
            PageHelper.clearPage();
        }
    }

    private Pageable resolvePageable(Object[] args) {
        return findPageable(args)
                .orElseGet(() -> PageRequest.of(0, properties.getDefaultPageSize()));
    }

    private Optional<Pageable> findPageable(Object[] args) {
        if (args == null) {
            return Optional.empty();
        }
        return Arrays.stream(args)
                .filter(Pageable.class::isInstance)
                .map(Pageable.class::cast)
                .findFirst();
    }

    private int clampSize(int requested, int annotationMax) {
        int absoluteMax = properties.getMaxPageSize();
        int upper = Math.min(annotationMax, absoluteMax);
        if (requested <= 0) {
            return Math.min(properties.getDefaultPageSize(), upper);
        }
        return Math.min(requested, upper);
    }

    /**
     * Decides whether and how to transform the method's raw return value.
     *
     * <p>The declared return type drives the decision so wrapping never
     * produces a {@code ClassCastException}:
     * <ul>
     *   <li>Declared {@code PageResponse<?>} or raw {@code Object}: wrap a
     *       {@code List} result into {@code PageResponse} (or call a
     *       {@link PageResponseFactory} bean if registered).</li>
     *   <li>Declared {@code List<?>}: return as-is. PageHelper's
     *       {@code Page<T>} <em>is</em> a {@code List}, so callers still get
     *       a sliced, sorted list — they just don't get the envelope.</li>
     *   <li>Anything else (Mono/Flux/POJO/void): pass through untouched. The
     *       reactive path is intentionally not handled here.</li>
     * </ul>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object adaptResult(Object result, Pageable pageable, Class<?> declaredReturnType) {
        if (result == null) {
            return null;
        }
        if (result instanceof PageResponse<?>) {
            return result; // Caller already produced the envelope.
        }
        if (!properties.isAutoWrapList()) {
            return result;
        }
        if (!(result instanceof List<?> list)) {
            if (log.isDebugEnabled()) {
                log.debug("AutoPaginate skipped wrapping non-List return type: {}",
                        result.getClass().getName());
            }
            return result;
        }
        boolean wrappingAllowed = PageResponse.class.isAssignableFrom(declaredReturnType)
                || declaredReturnType == Object.class;
        if (!wrappingAllowed) {
            // The method signature commits to returning a List (or some specific
            // subclass). Wrapping would break the cast — return the list verbatim.
            return list;
        }
        if (responseFactory != null) {
            PageResponse<?> envelope = PageResponse.from((List) list, pageable);
            return responseFactory.create(
                    envelope.content(),
                    pageable,
                    envelope.totalElements(),
                    envelope.totalPages());
        }
        return PageResponse.from((List) list, pageable);
    }
}
