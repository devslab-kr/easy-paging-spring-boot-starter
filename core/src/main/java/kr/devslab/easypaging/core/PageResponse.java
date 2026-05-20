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

    /**
     * Builds a response when the caller already knows the total element count
     * (e.g. from a separate count query — the typical R2DBC pattern, where the
     * page rows and the count come from two parallel {@code Mono}s).
     *
     * <p>Differs from {@link #from(List, Pageable)} which infers the total from
     * the PageHelper-wrapped {@code List} returned by a MyBatis mapper. Use
     * {@code from} when the {@code list} is a PageHelper {@code Page}; use
     * {@code of} when you have a plain {@code List} plus a separately computed
     * total.
     *
     * @param rows      the visible rows for this page (already trimmed to size)
     * @param pageable  the request that produced these rows
     * @param total     total elements across all pages
     */
    public static <T> PageResponse<T> of(List<T> rows, Pageable pageable, long total) {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(pageable, "pageable");
        if (total < 0) {
            throw new IllegalArgumentException("total must be >= 0 (use from() if unknown): " + total);
        }
        int pageNum = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        boolean first = pageNum == 0;
        boolean last = total == 0 || pageNum + 1 >= totalPages;
        return new PageResponse<>(
                rows,
                pageNum,
                size,
                total,
                totalPages,
                first,
                last,
                rows.isEmpty());
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

    /**
     * Returns a copy with the {@code page} field shifted by {@code +1}, for
     * APIs that expose 1-based page numbering to clients. Other fields are
     * untouched (so {@code totalPages}, {@code first}, {@code last} remain
     * computed against the original 0-based index, which is correct: the
     * count of pages and the first/last flags don't depend on the chosen
     * base).
     *
     * <p>Invoked by the aspect when {@code easy-paging.one-indexed-pages} is
     * enabled. Manual callers normally don't need this — define the API in
     * the convention you want and rely on the property.
     */
    public PageResponse<T> withOneIndexedPages() {
        return new PageResponse<>(
                content,
                page + 1,
                size,
                totalElements,
                totalPages,
                first,
                last,
                empty);
    }
}
