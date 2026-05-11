package kr.devslab.easypaging.core;

import com.github.pagehelper.Page;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Pageable;

/**
 * Immutable response envelope produced by the {@code @AutoPaginate} aspect.
 *
 * <p>Field shape intentionally matches Spring Data {@code Page} for client
 * compatibility, but this is a plain record (no Spring Data inheritance) so it
 * serializes cleanly via Jackson without bringing in Spring Data internals.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty) {

    /** {@code -1} indicates that the count query was skipped. */
    public static final long UNKNOWN_TOTAL = -1L;

    public PageResponse {
        Objects.requireNonNull(content, "content");
        content = List.copyOf(content);
    }

    /**
     * Builds a response from a PageHelper {@link Page} (the actual runtime type
     * of {@code List} returned by a MyBatis mapper after {@code PageHelper.startPage}).
     */
    public static <T> PageResponse<T> from(List<T> list, Pageable pageable) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(pageable, "pageable");

        if (list instanceof Page<?> page) {
            @SuppressWarnings("unchecked")
            Page<T> typed = (Page<T>) page;
            long total = typed.getTotal();
            int totalPages = typed.getPages();
            int pageNum = Math.max(0, typed.getPageNum() - 1);
            int pageSize = typed.getPageSize();
            boolean first = pageNum == 0;
            boolean last = total < 0 ? typed.size() < pageSize : pageNum + 1 >= totalPages;
            return new PageResponse<>(
                    typed,
                    pageNum,
                    pageSize,
                    total,
                    totalPages,
                    first,
                    last,
                    typed.isEmpty());
        }

        // Fallback: caller provided a plain List (PageHelper was not active for this query).
        int size = pageable.getPageSize();
        int pageNum = pageable.getPageNumber();
        return new PageResponse<>(
                list,
                pageNum,
                size,
                UNKNOWN_TOTAL,
                -1,
                pageNum == 0,
                list.size() < size,
                list.isEmpty());
    }

    public static <T> PageResponse<T> empty(Pageable pageable) {
        return new PageResponse<>(
                Collections.emptyList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                0L,
                0,
                true,
                true,
                true);
    }
}
