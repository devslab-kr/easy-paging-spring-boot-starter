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
}
