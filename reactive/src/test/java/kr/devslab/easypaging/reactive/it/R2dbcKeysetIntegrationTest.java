package kr.devslab.easypaging.reactive.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import kr.devslab.easypaging.core.Cursor;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.KeyColumn;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import reactor.test.StepVerifier;

@SpringBootTest(classes = ReactiveTestApplication.class)
class R2dbcKeysetIntegrationTest {

    @Autowired private R2dbcEntityTemplate template;

    // Unsigned codec — matches the test application.yml so signatures don't
    // interfere with what we're trying to verify (the keyset SQL itself).
    // JavaTimeModule is required for the Instant cursor key to roundtrip.
    private final CursorCodec codec = new CursorCodec(
            new ObjectMapper().registerModule(new JavaTimeModule()),
            "",
            2048);

    private static final List<KeyColumn> KEYS = List.of(
            new KeyColumn("created_at", "createdAt", Instant.class, SortDirection.DESC),
            new KeyColumn("id", "id", Long.class, SortDirection.DESC));

    @Test
    void firstForwardPageReturnsNewestRows() {
        KeysetRequest request = new KeysetRequest(Cursor.empty(), 3);

        StepVerifier.create(
                        R2dbcKeysetSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                KEYS,
                                request,
                                e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
                                codec))
                .assertNext((KeysetPage<TestEvent> page) -> {
                    // DESC sort, page size 3 → newest first: 10, 9, 8.
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(10L, 9L, 8L);
                    assertThat(page.hasNext()).isTrue();
                    assertThat(page.hasPrev()).isFalse();
                    assertThat(page.nextCursor()).isNotEmpty();
                    assertThat(page.prevCursor()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void forwardScanFollowsCursorIntoOlderRows() {
        // Cursor points to id=8 (the last item on the first page).
        KeysetRequest request = new KeysetRequest(
                Cursor.of(
                        Map.of("createdAt", "2026-01-08T00:00:00Z", "id", 8),
                        Cursor.Direction.FORWARD),
                3);

        StepVerifier.create(
                        R2dbcKeysetSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                KEYS,
                                request,
                                e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
                                codec))
                .assertNext(page -> {
                    // Should fetch ids strictly older than 8 → 7, 6, 5.
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(7L, 6L, 5L);
                    assertThat(page.hasNext()).isTrue();   // ids 4,3,2,1 still ahead
                    assertThat(page.hasPrev()).isTrue();   // a cursor was supplied → newer rows exist
                    assertThat(page.nextCursor()).isNotEmpty();
                    assertThat(page.prevCursor()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void backwardScanReturnsNewerRowsInDisplayOrder() {
        // Cursor points to id=5 going BACKWARD (toward newer rows).
        KeysetRequest request = new KeysetRequest(
                Cursor.of(
                        Map.of("createdAt", "2026-01-05T00:00:00Z", "id", 5),
                        Cursor.Direction.BACKWARD),
                3);

        StepVerifier.create(
                        R2dbcKeysetSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                KEYS,
                                request,
                                e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
                                codec))
                .assertNext(page -> {
                    // Three rows strictly newer than 5 → ids 6, 7, 8 in DESC display order: 8, 7, 6.
                    // (Mapper internally ORDER BY ASC + LIMIT 4, then KeysetPage.build reverses.)
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(8L, 7L, 6L);
                    assertThat(page.hasPrev()).isTrue();   // ids 9, 10 still newer
                    assertThat(page.hasNext()).isTrue();   // cursor implies at least row 5 is older
                })
                .verifyComplete();
    }

    @Test
    void backwardScanNearStartHasNoMoreNewerRows() {
        // Cursor points to id=8 going BACKWARD. Only ids 9 and 10 are newer
        // → 2 rows, hasPrev should be false (no more rows past these).
        KeysetRequest request = new KeysetRequest(
                Cursor.of(
                        Map.of("createdAt", "2026-01-08T00:00:00Z", "id", 8),
                        Cursor.Direction.BACKWARD),
                3);

        StepVerifier.create(
                        R2dbcKeysetSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                KEYS,
                                request,
                                e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
                                codec))
                .assertNext(page -> {
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(10L, 9L);
                    assertThat(page.hasPrev()).isFalse();  // no more newer rows
                    assertThat(page.hasNext()).isTrue();   // older rows still exist (id <= 8)
                    assertThat(page.prevCursor()).isNull();
                })
                .verifyComplete();
    }
}
