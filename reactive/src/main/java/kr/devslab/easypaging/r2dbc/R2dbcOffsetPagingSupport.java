package kr.devslab.easypaging.r2dbc;

import java.util.Objects;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

/**
 * Offset/limit pagination helper for Spring Data R2DBC.
 *
 * <p>R2DBC's typical paging pattern is two parallel queries: one for the page
 * rows (with {@code LIMIT}/{@code OFFSET}), one for the total row count. This
 * helper wires both into a single {@link Mono} that produces the same
 * {@link PageResponse} envelope as the servlet/MyBatis side, so a single API
 * contract works across the entire app.
 *
 * <pre>{@code
 * @GetMapping("/users")
 * public Mono<PageResponse<User>> list(Pageable pageable) {
 *     return R2dbcOffsetPagingSupport.paginate(
 *         template,
 *         User.class,
 *         Criteria.where("active").isTrue(),
 *         pageable);
 * }
 * }</pre>
 *
 * <p>For an unfiltered query pass {@link Criteria#empty()}. The
 * {@code Pageable}'s sort is honoured — pass {@code PageRequest.of(0, 20,
 * Sort.by("createdAt").descending())} to control ordering.
 */
public final class R2dbcOffsetPagingSupport {

    private R2dbcOffsetPagingSupport() {}

    /**
     * Returns a {@code Mono} that emits a fully populated {@link PageResponse}
     * for the entity type / criteria / pageable triple.
     *
     * <p>Implementation: runs the page-rows query and the count query in
     * parallel via {@link Mono#zip} so they share latency rather than stack.
     * The count query reuses the same {@code criteria} (without the limit/
     * offset of course) — be sure the criteria captures the same filter your
     * UI exposes, or {@code totalElements} won't match what users can scroll
     * through.
     */
    public static <T> Mono<PageResponse<T>> paginate(R2dbcEntityTemplate template,
                                                      Class<T> entityClass,
                                                      Criteria criteria,
                                                      Pageable pageable) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(entityClass, "entityClass");
        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(pageable, "pageable");

        Query rowsQuery = Query.query(criteria).with(pageable);
        Query countQuery = Query.query(criteria);

        Mono<java.util.List<T>> rows = template.select(entityClass)
                .matching(rowsQuery)
                .all()
                .collectList();
        Mono<Long> total = template.count(countQuery, entityClass);

        return Mono.zip(rows, total, (list, count) -> PageResponse.of(list, pageable, count));
    }
}
