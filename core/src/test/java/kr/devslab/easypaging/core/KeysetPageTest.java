package kr.devslab.easypaging.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeysetPageTest {

    private final CursorCodec codec = new CursorCodec(new ObjectMapper(), "", 2048);

    record Row(long id, String time) {}

    @Test
    void buildDetectsNextPageViaSizePlusOne() {
        // Mapper queried with size+1=4 and returned 4 rows → there's a next page.
        List<Row> rows = List.of(
                new Row(10, "2026-05-01T03:00:00Z"),
                new Row(9, "2026-05-01T02:00:00Z"),
                new Row(8, "2026-05-01T01:00:00Z"),
                new Row(7, "2026-05-01T00:00:00Z"));
        KeysetRequest request = new KeysetRequest(Cursor.empty(), 3);

        KeysetPage<Row> page = KeysetPage.build(
                rows, request,
                r -> Map.of("id", r.id(), "time", r.time()),
                codec);

        assertThat(page.content()).hasSize(3);
        assertThat(page.content()).extracting(Row::id).containsExactly(10L, 9L, 8L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isNotNull().isNotEmpty();

        Cursor decoded = codec.decode(page.nextCursor());
        assertThat(decoded.keys()).containsEntry("id", 8);
    }

    @Test
    void buildReturnsAllRowsWhenLessThanSizePlusOne() {
        List<Row> rows = List.of(new Row(2, "t2"), new Row(1, "t1"));
        KeysetRequest request = new KeysetRequest(Cursor.empty(), 10);

        KeysetPage<Row> page = KeysetPage.build(
                rows, request,
                r -> Map.of("id", r.id()),
                codec);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void buildMarksHasPrevWhenCursorWasProvided() {
        List<Row> rows = List.of(new Row(5, "t5"));
        KeysetRequest request = new KeysetRequest(
                Cursor.of(Map.of("id", 10), Cursor.Direction.FORWARD),
                3);

        KeysetPage<Row> page = KeysetPage.build(
                rows, request,
                r -> Map.of("id", r.id()),
                codec);

        assertThat(page.hasPrev()).isTrue();
    }

    @Test
    void forwardWithCursorEmitsPrevCursorEncodedAsBackward() {
        // FORWARD scan from a cursor → prevCursor should now exist (head of
        // page, BACKWARD direction) so the client can navigate "newer".
        List<Row> rows = List.of(
                new Row(7, "t7"),
                new Row(6, "t6"),
                new Row(5, "t5"),
                new Row(4, "t4"));
        KeysetRequest request = new KeysetRequest(
                Cursor.of(Map.of("id", 8L), Cursor.Direction.FORWARD), 3);

        KeysetPage<Row> page = KeysetPage.build(
                rows, request,
                r -> Map.of("id", r.id()),
                codec);

        assertThat(page.content()).extracting(Row::id).containsExactly(7L, 6L, 5L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrev()).isTrue();
        assertThat(page.nextCursor()).isNotNull();
        assertThat(page.prevCursor()).isNotNull();

        Cursor decodedPrev = codec.decode(page.prevCursor());
        assertThat(decodedPrev.direction()).isEqualTo(Cursor.Direction.BACKWARD);
        assertThat(decodedPrev.keys()).containsEntry("id", 7);   // head of page

        Cursor decodedNext = codec.decode(page.nextCursor());
        assertThat(decodedNext.direction()).isEqualTo(Cursor.Direction.FORWARD);
        assertThat(decodedNext.keys()).containsEntry("id", 5);   // tail of page (size+1 row trimmed)
    }

    @Test
    void backwardScanReversesMapperOrderToDisplayOrder() {
        // BACKWARD scan: mapper returns rows in ASC order, build() flips to
        // DESC for display.
        List<Row> mapperRows = List.of(
                new Row(6, "t6"),
                new Row(7, "t7"),
                new Row(8, "t8"));   // size=3, no +1 → no more newer rows
        KeysetRequest request = new KeysetRequest(
                Cursor.of(Map.of("id", 5L), Cursor.Direction.BACKWARD), 3);

        KeysetPage<Row> page = KeysetPage.build(
                mapperRows, request,
                r -> Map.of("id", r.id()),
                codec);

        // Display order: newest first
        assertThat(page.content()).extracting(Row::id).containsExactly(8L, 7L, 6L);
        assertThat(page.hasNext()).isTrue();   // cursor was supplied → older rows exist
        assertThat(page.hasPrev()).isFalse();  // no +1 → no more newer
        assertThat(page.prevCursor()).isNull();
        assertThat(page.nextCursor()).isNotNull();

        // nextCursor (= "older") should encode the bottom-of-display row.
        Cursor decodedNext = codec.decode(page.nextCursor());
        assertThat(decodedNext.direction()).isEqualTo(Cursor.Direction.FORWARD);
        assertThat(decodedNext.keys()).containsEntry("id", 6);
    }

    @Test
    void backwardWithMoreRowsEncodesPrevCursorFromHead() {
        // BACKWARD scan with size+1 rows → more newer rows exist.
        // Mapper returns ASC: [6, 7, 8, 9] for size=3.
        // After trim of the LAST mapper row (the next-newer marker, row 9):
        //   kept (ASC) = [6, 7, 8]
        //   reversed for display = [8, 7, 6]
        List<Row> mapperRows = List.of(
                new Row(6, "t6"),
                new Row(7, "t7"),
                new Row(8, "t8"),
                new Row(9, "t9"));
        KeysetRequest request = new KeysetRequest(
                Cursor.of(Map.of("id", 5L), Cursor.Direction.BACKWARD), 3);

        KeysetPage<Row> page = KeysetPage.build(
                mapperRows, request,
                r -> Map.of("id", r.id()),
                codec);

        assertThat(page.content()).extracting(Row::id).containsExactly(8L, 7L, 6L);
        assertThat(page.hasPrev()).isTrue();   // +1 trick triggered → more newer
        assertThat(page.hasNext()).isTrue();   // cursor was supplied → older rows exist

        // prevCursor (= "newer") encodes the TOP-of-display row, BACKWARD direction.
        // A follow-up BACKWARD scan from id=8 returns rows 9, 10, ... (more newer).
        Cursor decodedPrev = codec.decode(page.prevCursor());
        assertThat(decodedPrev.direction()).isEqualTo(Cursor.Direction.BACKWARD);
        assertThat(decodedPrev.keys()).containsEntry("id", 8);

        // nextCursor (= "older") encodes the BOTTOM-of-display row, FORWARD.
        Cursor decodedNext = codec.decode(page.nextCursor());
        assertThat(decodedNext.direction()).isEqualTo(Cursor.Direction.FORWARD);
        assertThat(decodedNext.keys()).containsEntry("id", 6);
    }
}
