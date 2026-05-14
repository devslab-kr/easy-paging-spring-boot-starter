package kr.devslab.easypaging.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises {@code @AutoPaginate} through the full Spring MVC stack.
 *
 * <p>Covers behaviors that integration via {@code TestUserService} alone can't
 * reach: the controller-side argument resolution of {@code Pageable} from
 * query params, error-to-status mapping, and ThreadLocal cleanup after
 * exceptions thrown mid-flight.
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class AutoPaginateWebMvcIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void happyPathReturnsPagedJson() throws Exception {
        mockMvc.perform(get("/test/auto/users").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    void multiColumnSortIsApplied() throws Exception {
        mockMvc.perform(get("/test/auto/users")
                        .param("page", "0")
                        .param("size", "3")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.content[1].id").value(11))
                .andExpect(jsonPath("$.content[2].id").value(10));
    }

    @Test
    void sqlInjectionInSortReturnsBadRequest() throws Exception {
        // SortConverter rejects identifiers containing semicolons / parens / etc.
        // The aspect translates that into a 400 instead of letting it become a 500.
        mockMvc.perform(get("/test/auto/users").param("sort", "name;DROP TABLE users,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortDoesNotLeakPageHelperState() throws Exception {
        // Trigger the bad-sort path. The aspect calls PageHelper.startPage before
        // SortConverter runs, so we have to verify the finally block cleared the
        // ThreadLocal even though the method body never executed.
        mockMvc.perform(get("/test/auto/users").param("sort", "$(rm -rf /),desc"))
                .andExpect(status().isBadRequest());

        // If startPage had leaked, the next sync MyBatis query on this thread
        // would inherit it. We can verify directly via PageHelper's local state.
        org.assertj.core.api.Assertions.assertThat(PageHelper.getLocalPage()).isNull();
    }

    @Test
    void oversizedRequestIsClampedNotRejected() throws Exception {
        // maxSize on the service is 50; we ask for 9999 — should be clamped
        // silently, not return 400.
        mockMvc.perform(get("/test/auto/users").param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }
}
