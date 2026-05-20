package kr.devslab.easypaging.reactive.it.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import kr.devslab.easypaging.core.Cursor;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetRequest;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.KeyColumn;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.SortDirection;
import kr.devslab.easypaging.r2dbc.R2dbcOffsetPagingSupport;
import kr.devslab.easypaging.reactive.it.ReactiveTestApplication;
import kr.devslab.easypaging.reactive.it.TestEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

/**
 * Dialect-compat smoke test: the R2DBC helpers work against real PostgreSQL.
 *
 * <p>Specifically focused on the cases the H2 driver lets us cheat on:
 * <ul>
 *   <li>{@code TIMESTAMP WITH TIME ZONE} → {@link Instant} binding for keyset
 *       cursor values (the conversion path we hit during development).</li>
 *   <li>{@code BIGINT} → {@link Long} round-tripping for the cursor's id
 *       tiebreaker.</li>
 *   <li>R2DBC postgres driver's {@code COUNT(*)} returning {@code Long}
 *       (vs H2's potentially returning {@code Integer}).</li>
 * </ul>
 *
 * <p>The init script that creates the schema runs inside the container, not
 * via the application's bean — so we exclude
 * {@link kr.devslab.easypaging.reactive.it.ReactiveTestApplication}'s
 * {@code ConnectionFactoryInitializer} via a fresh {@link SpringBootTest}
 * with only the auto-configurations + our test controller pulled in.
 */
@SpringBootTest(classes = ReactiveTestApplication.class)
@Import(R2dbcPostgresDialectTest.DisableEmbeddedInitializerConfig.class)
@Tag("dialect-compat")
@Testcontainers
class R2dbcPostgresDialectTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("easy_paging_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("dialect/postgres-schema.sql");

    @DynamicPropertySource
    static void r2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) +
                "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Autowired private R2dbcEntityTemplate template;

    private final CursorCodec codec = new CursorCodec(
            new ObjectMapper().registerModule(new JavaTimeModule()),
            "",
            2048);

    private static final List<KeyColumn> KEYS = List.of(
            new KeyColumn("created_at", "createdAt", Instant.class, SortDirection.DESC),
            new KeyColumn("id", "id", Long.class, SortDirection.DESC));

    @Test
    void offsetPaginationCountsCorrectlyOnPostgres() {
        StepVerifier.create(
                        R2dbcOffsetPagingSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                PageRequest.of(0, 3, Sort.by("id").ascending())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(3);
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(1L, 2L, 3L);
                    assertThat(page.totalElements()).isEqualTo(10L);
                    assertThat(page.totalPages()).isEqualTo(4);
                })
                .verifyComplete();
    }

    @Test
    void keysetForwardScanBindsInstantToTimestamptzColumn() {
        // This is the failure mode we want to lock in: when the cursor JSON
        // round-trips an Instant through a String, the helper must coerce it
        // back to Instant before binding. r2dbc-postgresql rejects a
        // CHARACTER LARGE OBJECT bind for a TIMESTAMP WITH TIME ZONE column
        // (the same way r2dbc-h2 did during development). If this regresses,
        // we'll see "Data conversion error" instead of a 200.
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
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(7L, 6L, 5L);
                    assertThat(page.hasNext()).isTrue();
                    assertThat(page.hasPrev()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void keysetBackwardScanReversesIntoDisplayOrderOnPostgres() {
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
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(8L, 7L, 6L);
                })
                .verifyComplete();
    }

    /**
     * Replaces the application's {@code ConnectionFactoryInitializer} (which
     * tries to load the H2-shaped {@code schema.sql} + {@code data.sql}) with
     * a no-op. The Testcontainers init script already populated the schema
     * before Spring even started.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class DisableEmbeddedInitializerConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer noopInitializer(
                io.r2dbc.spi.ConnectionFactory factory) {
            var initializer = new org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer();
            initializer.setConnectionFactory(factory);
            initializer.setDatabasePopulator(connection -> reactor.core.publisher.Mono.empty());
            return initializer;
        }
    }
}
