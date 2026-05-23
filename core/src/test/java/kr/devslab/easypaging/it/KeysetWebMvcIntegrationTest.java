package kr.devslab.easypaging.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
        String nextCursor = firstJson.get("nextCursor").asString();
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

    @Test
    void forwardPageAfterFirstNowExposesPrevCursor() throws Exception {
        // Before v0.3: prevCursor was always null even mid-stream.
        // After v0.3: prevCursor is populated for any page that isn't the first,
        // encoding the head row with BACKWARD direction so the client can
        // navigate "newer" without having to keep track of where they came from.
        MvcResult firstResult = mockMvc.perform(get("/test/users/keyset"))
                .andExpect(jsonPath("$.prevCursor").isEmpty())
                .andReturn();
        String firstNext = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("nextCursor").asString();

        mockMvc.perform(get("/test/users/keyset").param("cursor", firstNext))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.prevCursor").isNotEmpty());
    }

    @Test
    void backwardScanReturnsRowsInDisplayOrder() throws Exception {
        // To trigger a backward scan, first get a cursor from the middle of
        // the stream, then use its prevCursor (which encodes BACKWARD direction).
        MvcResult firstResult = mockMvc.perform(get("/test/users/keyset")).andReturn();
        String firstNext = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("nextCursor").asString();

        MvcResult secondResult = mockMvc.perform(get("/test/users/keyset").param("cursor", firstNext))
                .andReturn();
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        String prevCursor = secondJson.get("prevCursor").asString();
        long secondHeadId = secondJson.get("content").get(0).get("id").asLong();

        // Going back via prevCursor should return rows newer than secondHeadId,
        // and the content list must still be in DESC display order.
        MvcResult backResult = mockMvc.perform(get("/test/users/keyset").param("cursor", prevCursor))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode backJson = objectMapper.readTree(backResult.getResponse().getContentAsString());

        long firstId = backJson.get("content").get(0).get("id").asLong();
        long lastId = backJson.get("content").get(backJson.get("content").size() - 1).get("id").asLong();

        org.assertj.core.api.Assertions.assertThat(firstId).isGreaterThan(lastId);
        org.assertj.core.api.Assertions.assertThat(lastId).isGreaterThan(secondHeadId);
    }

    @Test
    void bidirectionalRoundtripLandsOnSamePage() throws Exception {
        // page 1 → take nextCursor → page 2 → take prevCursor → land back on
        // a page whose content equals page 1 (minus pagination artifacts).
        MvcResult page1 = mockMvc.perform(get("/test/users/keyset")).andReturn();
        JsonNode page1Json = objectMapper.readTree(page1.getResponse().getContentAsString());

        MvcResult page2 = mockMvc.perform(get("/test/users/keyset")
                        .param("cursor", page1Json.get("nextCursor").asString()))
                .andReturn();
        JsonNode page2Json = objectMapper.readTree(page2.getResponse().getContentAsString());

        MvcResult back = mockMvc.perform(get("/test/users/keyset")
                        .param("cursor", page2Json.get("prevCursor").asString()))
                .andReturn();
        JsonNode backJson = objectMapper.readTree(back.getResponse().getContentAsString());

        // Content equality (id-by-id) means we navigated to the same rows.
        org.assertj.core.api.Assertions.assertThat(backJson.get("content").toString())
                .isEqualTo(page1Json.get("content").toString());
    }

    @Test
    void backwardScanHasNextTrueAndHasPrevFalseWhenAtNewestEnd() throws Exception {
        // Walk to page 2, then use prevCursor. Because the dataset has only
        // 12 rows and page 1 already showed the newest 3, going back from
        // page 2 lands on page 1 with no newer rows remaining.
        MvcResult page1 = mockMvc.perform(get("/test/users/keyset")).andReturn();
        String page1Next = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("nextCursor").asString();
        MvcResult page2 = mockMvc.perform(get("/test/users/keyset").param("cursor", page1Next))
                .andReturn();
        String page2Prev = objectMapper.readTree(page2.getResponse().getContentAsString())
                .get("prevCursor").asString();

        mockMvc.perform(get("/test/users/keyset").param("cursor", page2Prev))
                .andExpect(jsonPath("$.hasNext").value(true))    // older rows still exist
                .andExpect(jsonPath("$.hasPrev").value(false))   // no more newer
                .andExpect(jsonPath("$.prevCursor").isEmpty());
    }

    @Test
    void explicitDirectionParamOverridesCursorEncodedDirection() throws Exception {
        // The cursor token already encodes direction, but the explicit
        // ?direction= query param must still take precedence — this lets
        // clients re-purpose a cursor without re-encoding it server-side.
        MvcResult page1 = mockMvc.perform(get("/test/users/keyset")).andReturn();
        String forwardCursor = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("nextCursor").asString();  // encoded as FORWARD

        // Use that forward cursor but tell the resolver BACKWARD.
        // The page should walk back toward newer items (the rows above id=10).
        MvcResult result = mockMvc.perform(get("/test/users/keyset")
                        .param("cursor", forwardCursor)
                        .param("direction", "BACKWARD"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        // The returned rows must all have id > 10 (the forward cursor's key)
        // because backward means "items newer than that boundary".
        long page1LastId = objectMapper.readTree(page1.getResponse().getContentAsString())
                .get("content").get(2).get("id").asLong();
        for (JsonNode row : json.get("content")) {
            org.assertj.core.api.Assertions.assertThat(row.get("id").asLong())
                    .isGreaterThan(page1LastId);
        }
    }
}
