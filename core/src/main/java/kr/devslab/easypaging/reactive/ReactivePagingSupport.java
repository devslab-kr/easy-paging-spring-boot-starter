package kr.devslab.easypaging.reactive;

import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.function.Supplier;
import kr.devslab.easypaging.core.PageResponse;
import kr.devslab.easypaging.support.SortConverter;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Bridge helpers for using PageHelper from reactive (Project Reactor) code.
 *
 * <p>PageHelper relies on {@link ThreadLocal}, which does not propagate across
 * Reactor scheduler hops. The pattern below wraps the entire startPage →
 * mapper-call → clearPage triple inside a single {@link Mono#fromCallable}
 * subscribed on a blocking-IO scheduler, ensuring ThreadLocal consistency.
 *
 * <pre>{@code
 * @GetMapping("/reports")
 * public Mono<PageResponse<Report>> list(Pageable pageable) {
 *     return ReactivePagingSupport.paginate(
 *         pageable,
 *         () -> reportMapper.findAll(),
 *         100,
 *         true);
 * }
 * }</pre>
 *
 * <p>Loaded only when {@code reactor-core} is on the classpath (see
 * {@link kr.devslab.easypaging.autoconfigure.ReactiveEasyPagingAutoConfiguration}).
 */
public final class ReactivePagingSupport {

    private ReactivePagingSupport() {}

    public static <T> Mono<PageResponse<T>> paginate(Pageable pageable,
                                                      Supplier<List<T>> mapperCall,
                                                      int maxSize,
                                                      boolean count) {
        return paginate(pageable, mapperCall, maxSize, count, Schedulers.boundedElastic());
    }

    public static <T> Mono<PageResponse<T>> paginate(Pageable pageable,
                                                      Supplier<List<T>> mapperCall,
                                                      int maxSize,
                                                      boolean count,
                                                      Scheduler scheduler) {
        return Mono.fromCallable(() -> {
                    int pageNum = Math.max(0, pageable.getPageNumber()) + 1;
                    int size = Math.min(Math.max(1, pageable.getPageSize()), maxSize);
                    PageHelper.startPage(pageNum, size, count, /* reasonable */ Boolean.TRUE, Boolean.FALSE);
                    String orderBy = SortConverter.toOrderBy(pageable.getSort());
                    if (!orderBy.isEmpty()) {
                        PageHelper.orderBy(orderBy);
                    }
                    try {
                        List<T> list = mapperCall.get();
                        return PageResponse.from(list, pageable);
                    } finally {
                        PageHelper.clearPage();
                    }
                })
                .subscribeOn(scheduler);
    }
}
