package kr.devslab.easypaging.it.dialect;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.devslab.easypaging.it.TestApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Dialect-compat smoke test: {@code @AutoPaginate} works against real
 * PostgreSQL.
 *
 * <p>The H2 tests in {@link kr.devslab.easypaging.it.AutoPaginateWebMvcIntegrationTest}
 * cover the controller / aspect / response shape exhaustively in seconds.
 * This test exists to catch the things H2 won't: PageHelper's
 * PostgreSQL-specific {@code LIMIT/OFFSET} rewriting, transaction isolation
 * differences, and PostgreSQL's stricter type coercion at the JDBC layer.
 *
 * <p>Tagged {@code dialect-compat} — runs via the {@code testDialect} Gradle
 * task. Requires a Docker daemon.
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@Tag("dialect-compat")
@Testcontainers
class AutoPaginatePostgresDialectTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("easy_paging_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("dialect/postgres-schema.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        // Switch off the default in-memory H2 schema/data SQL so PostgreSQL's
        // init script (above) is the only source of truth for this test.
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired private MockMvc mockMvc;

    @Test
    void offsetPaginationWorksAgainstPostgres() throws Exception {
        mockMvc.perform(get("/test/auto/users").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void sortParameterIsRewrittenSafelyByPageHelper() throws Exception {
        // Exercises PageHelper's ORDER BY injection on PostgreSQL specifically.
        // Column name is camelCase in Java but snake_case in SQL — verify the
        // SortConverter + PageHelper roundtrip doesn't double-quote or mangle.
        mockMvc.perform(get("/test/auto/users")
                        .param("page", "0")
                        .param("size", "3")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void lastPageReportsCorrectSizeOnPostgres() throws Exception {
        // 15 rows / size 5 → page 2 is the last page with 5 rows.
        mockMvc.perform(get("/test/auto/users").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.first").value(false));
    }
}
