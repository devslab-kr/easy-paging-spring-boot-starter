package kr.devslab.easypaging.spi;

import java.util.List;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * SPI for replacing the default {@link PageResponse} envelope with a custom
 * response shape (e.g. a legacy company-wide wrapper).
 *
 * <p>Register a bean implementing this interface; the aspect will route the
 * result through it instead of constructing {@code PageResponse} directly.
 *
 * <pre>{@code
 * @Bean
 * public PageResponseFactory companyEnvelope() {
 *     return new PageResponseFactory() {
 *         @Override
 *         public Object create(List<?> content, Pageable pageable, long totalElements, int totalPages) {
 *             return Map.of(
 *                 "ok", true,
 *                 "data", content,
 *                 "page", Map.of("current", pageable.getPageNumber(), "size", pageable.getPageSize(),
 *                                "total", totalElements));
 *         }
 *     };
 * }
 * }</pre>
 */
@FunctionalInterface
public interface PageResponseFactory {

    /**
     * @param content        the page slice (already trimmed by PageHelper)
     * @param pageable       the resolved request pageable
     * @param totalElements  total matching rows, or {@code -1} if count was skipped
     * @param totalPages     total page count, or {@code -1} if count was skipped
     * @return the object the controller should ultimately return
     */
    Object create(List<?> content, Pageable pageable, long totalElements, int totalPages);
}
