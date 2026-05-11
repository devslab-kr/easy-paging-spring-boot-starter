package kr.devslab.easypaging.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Response envelope for keyset-paginated endpoints.
 *
 * <p>The {@code nextCursor} is non-null when more rows exist past this page;
 * the client passes it back as {@code ?cursor=...} for the next request.
 * {@code prevCursor} is currently informational and only populated when the
 * caller drove a {@code BACKWARD} scan.
 */
public record KeysetPage<T>(
        List<T> content,
        int size,
        String nextCursor,
        String prevCursor,
        boolean hasNext,
        boolean hasPrev) {

    public KeysetPage {
        Objects.requireNonNull(content, "content");
        content = List.copyOf(content);
    }

    public static <T> KeysetPage<T> empty(int size) {
        return new KeysetPage<>(Collections.emptyList(), size, null, null, false, false);
    }

    /**
     * Builds a keyset page from rows fetched with {@code limit = request.size() + 1}.
     *
     * <p>The "+1" trick lets us detect a next page without a second query:
     * if the mapper returned {@code size + 1} rows we know more exist, and we
     * strip the extra row before encoding the cursor of the last visible row.
     *
     * @param rows           rows returned by the mapper, length up to {@code request.size() + 1}
     * @param request        the resolved request that produced these rows
     * @param keyExtractor   extracts the cursor key map from the last visible row
     * @param codec          codec used to encode the next cursor
     */
    public static <T> KeysetPage<T> build(
            List<T> rows,
            KeysetRequest request,
            Function<T, Map<String, Object>> keyExtractor,
            CursorCodec codec) {

        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        Objects.requireNonNull(codec, "codec");

        int size = request.size();
        boolean hasMore = rows.size() > size;
        List<T> visible = hasMore ? List.copyOf(rows.subList(0, size)) : List.copyOf(rows);

        String nextCursor = null;
        if (hasMore && !visible.isEmpty()) {
            T tail = visible.get(visible.size() - 1);
            Map<String, Object> keys = keyExtractor.apply(tail);
            nextCursor = codec.encode(Cursor.of(keys, Cursor.Direction.FORWARD));
        }

        boolean hasPrev = !request.isFirstPage();
        String prevCursor = null; // Symmetrical prev requires a separate query; deferred to a future release.

        return new KeysetPage<>(visible, size, nextCursor, prevCursor, hasMore, hasPrev);
    }
}
