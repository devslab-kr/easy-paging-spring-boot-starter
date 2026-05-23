package kr.devslab.easypaging.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the {@code easy-paging.one-indexed-pages=true} contract end-to-end.
 *
 * <p>With this property on:
 * <ul>
 *   <li>Spring Data Web's {@code PageableHandlerMethodArgumentResolver} is told
 *       to interpret {@code ?page=1} as the first page (translates to
 *       {@code Pageable(pageNumber=0)} internally).</li>
 *   <li>The aspect shifts the response {@code page} field by {@code +1} so the
 *       client sees the same convention on the way out.</li>
 * </ul>
 *
 * <p>The default 0-based behavior is covered by
 * {@link AutoPaginateWebMvcIntegrationTest}, which runs without this property
 * set.
 */
@SpringBootTest(
        classes = TestApplication.class,
        properties = "easy-paging.one-indexed-pages=true")
@AutoConfigureMockMvc
class OneIndexedPagesIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void pageOneIsTheFirstPage() throws Exception {
        // Seed has 12 rows (alice..niaj). With size=5 and 1-based numbering,
        // ?page=1 should return the first 5 rows.
        mockMvc.perform(get("/test/auto/users").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].name").value("alice"))
                .andExpect(jsonPath("$.content[4].name").value("eve"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void pageTwoReturnsSecondSlice() throws Exception {
        mockMvc.perform(get("/test/auto/users").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("frank"))
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void pageThreeIsLastAndPartial() throws Exception {
        mockMvc.perform(get("/test/auto/users").param("page", "3").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(3));
    }
}
