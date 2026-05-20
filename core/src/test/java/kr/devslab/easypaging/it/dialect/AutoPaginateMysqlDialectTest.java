package kr.devslab.easypaging.it.dialect;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.devslab.easypaging.it.TestApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Dialect-compat smoke test: {@code @AutoPaginate} works against real MySQL.
 *
 * <p>Companion to {@link AutoPaginatePostgresDialectTest}. PageHelper's
 * MySQL rewriter is the most-used dialect in practice, so a smoke check here
 * catches the obvious "PageHelper picked the wrong dialect from the JDBC
 * URL" class of regression.
 *
 * <p>Tagged {@code dialect-compat} — runs via the {@code testDialect} Gradle
 * task. Requires a Docker daemon.
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@Tag("dialect-compat")
@Testcontainers
class AutoPaginateMysqlDialectTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("easy_paging_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("dialect/mysql-schema.sql");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired private MockMvc mockMvc;

    @Test
    void offsetPaginationWorksAgainstMysql() throws Exception {
        mockMvc.perform(get("/test/auto/users").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void midPageReportsBoundaryFlagsCorrectly() throws Exception {
        mockMvc.perform(get("/test/auto/users").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void sortDescOrderIsHonouredOnMysql() throws Exception {
        // Top of created_at DESC sort should yield the newest seeded row first.
        mockMvc.perform(get("/test/auto/users")
                        .param("page", "0")
                        .param("size", "3")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("quentin"));
    }
}
