package kr.devslab.easypaging.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Response envelope for keyset-paginated endpoints.
 *
 * <p>The cursor fields use <em>user-perspective</em> semantics that stay the
 * same regardless of which direction the request scanned:
 * <ul>
 *   <li>{@code nextCursor} — load the <em>next page in display order</em>
 *       (i.e. older items when the natural sort is DESC by time). Always
 *       encoded with {@link Cursor.Direction#FORWARD}.</li>
 *   <li>{@code prevCursor} — load the <em>previous page in display order</em>
 *       (i.e. newer items). Always encoded with
 *       {@link Cursor.Direction#BACKWARD}.</li>
 * </ul>
 * <p>This means the client can blindly use {@code ?cursor=nextCursor} /
 * {@code ?cursor=prevCursor} for forward/backward navigation — the cursor
 * itself carries the direction, so the client never has to think about the
 * request's previous direction.
 *
 * <p>{@code hasNext} / {@code hasPrev} mirror the cursor fields: "are there
 * more rows in display order beyond this page?" / "...before this page?".
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
     * <h2>Forward scans (default)</h2>
     * <p>The mapper returns rows already in display order (e.g. {@code ORDER BY
     * time DESC, id DESC}). The "+1" trick lets us detect a next page without
     * a second query: if the mapper returned {@code size + 1} rows we know
     * more older rows exist, and we strip the extra row before encoding the
     * cursor of the last visible row.
     *
     * <h2>Backward scans ({@code request.direction() == BACKWARD})</h2>
     * <p>The mapper is expected to return rows in <em>reverse</em> display
     * order (e.g. {@code ORDER BY time ASC, id ASC} when the user-facing view
     * is DESC). This is because the natural way to express "rows newer than
     * the cursor" is {@code WHERE time > cursor ORDER BY time ASC}. The build
     * helper trims the extra row from the tail (in mapper order, that's the
     * row furthest from the cursor), then reverses the remaining rows so the
     * returned {@code content} list is in user-facing display order — the
     * same order the consumer would see from a forward scan covering the same
     * range. The cursor encoding then proceeds identically.
     *
     * @param rows           rows returned by the mapper, length up to
     *                       {@code request.size() + 1}. For {@code BACKWARD}
     *                       scans, expected to be in reverse display order
     *                       (the natural order of the {@code WHERE time >
     *                       cursor ORDER BY time ASC} query).
     * @param request        the resolved request that produced these rows
     * @param keyExtractor   extracts the cursor key map from a row
     * @param codec          codec used to encode cursor tokens
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
        Cursor.Direction direction = request.direction();
        boolean hasMore = rows.size() > size;

        // Trim the "+1" row first (always at the tail of the mapper's result —
        // for FORWARD that's the next older row, for BACKWARD that's the next
        // newer row). What remains is the page content in mapper order.
        List<T> kept = hasMore ? new ArrayList<>(rows.subList(0, size)) : new ArrayList<>(rows);

        // For BACKWARD scans the mapper returned rows in reverse display order,
        // so flip them to match the user's view (newest first when sort is DESC).
        if (direction == Cursor.Direction.BACKWARD) {
            Collections.reverse(kept);
        }
        List<T> visible = List.copyOf(kept);

        // From here on, `visible` is in display order regardless of scan direction:
        //   head = page top    (newer in a DESC view)
        //   tail = page bottom (older in a DESC view)
        // nextCursor / prevCursor semantics are direction-invariant:
        //   nextCursor → continue toward "more older" (FORWARD direction encoded)
        //   prevCursor → continue toward "more newer" (BACKWARD direction encoded)
        String nextCursor = null;
        String prevCursor = null;

        if (!visible.isEmpty()) {
            T head = visible.get(0);
            T tail = visible.get(visible.size() - 1);

            boolean hasMoreOlder;
            boolean hasMoreNewer;
            if (direction == Cursor.Direction.FORWARD) {
                hasMoreOlder = hasMore;                       // +1 trick proves it
                hasMoreNewer = !request.isFirstPage();        // a cursor was supplied
            } else {
                hasMoreOlder = !request.isFirstPage();        // a cursor was supplied
                hasMoreNewer = hasMore;                       // +1 trick proves it
            }

            if (hasMoreOlder) {
                nextCursor = codec.encode(
                        Cursor.of(keyExtractor.apply(tail), Cursor.Direction.FORWARD));
            }
            if (hasMoreNewer) {
                prevCursor = codec.encode(
                        Cursor.of(keyExtractor.apply(head), Cursor.Direction.BACKWARD));
            }
        }

        boolean hasNext;
        boolean hasPrev;
        if (direction == Cursor.Direction.FORWARD) {
            hasNext = hasMore;
            hasPrev = !request.isFirstPage();
        } else {
            hasNext = !request.isFirstPage();
            hasPrev = hasMore;
        }

        return new KeysetPage<>(visible, size, nextCursor, prevCursor, hasNext, hasPrev);
    }
}
