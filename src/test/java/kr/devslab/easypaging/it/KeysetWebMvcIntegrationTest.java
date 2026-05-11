package kr.devslab.easypaging.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class KeysetWebMvcIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void firstPageReturnsThreeRowsAndACursor() throws Exception {
        mockMvc.perform(get("/test/users/keyset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());
    }

    @Test
    void requestedSizeIsClampedByMaxSize() throws Exception {
        // Annotation says maxSize=50; we ask for 9999.
        mockMvc.perform(get("/test/users/keyset").param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void followingTheCursorAdvancesPages() throws Exception {
        MvcResult firstResult = mockMvc.perform(get("/test/users/keyset"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        String nextCursor = firstJson.get("nextCursor").asText();
        long firstPageLastId = firstJson.get("content").get(2).get("id").asLong();

        MvcResult secondResult = mockMvc.perform(get("/test/users/keyset").param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andReturn();

        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        long secondPageFirstId = secondJson.get("content").get(0).get("id").asLong();

        // The second page must start where the first ended (strictly less, DESC).
        org.assertj.core.api.Assertions.assertThat(secondPageFirstId).isLessThan(firstPageLastId);
    }

    @Test
    void garbageCursorTreatedAsFirstPage() throws Exception {
        // decodeOrEmpty fallback path: invalid cursor must not 500.
        mockMvc.perform(get("/test/users/keyset").param("cursor", "!!!not-real!!!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }
}
