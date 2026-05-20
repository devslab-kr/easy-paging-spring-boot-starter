package kr.devslab.easypaging.reactive.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * End-to-end test: HTTP request → WebFlux argument resolver →
 * {@code R2dbcKeysetSupport} → R2DBC H2 → JSON response. Verifies the full
 * reactive stack wires together correctly.
 */
@SpringBootTest(classes = ReactiveTestApplication.class)
@AutoConfigureWebTestClient
class ReactiveKeysetWebFluxIntegrationTest {

    @Autowired private WebTestClient client;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void firstPageHits3RowsAndPopulatesNextCursor() throws Exception {
        EntityExchangeResult<byte[]> result = client.get()
                .uri("/test/events/keyset")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        JsonNode body = objectMapper.readTree(result.getResponseBody());
        assertThat(body.get("content").size()).isEqualTo(3);
        assertThat(body.get("content").get(0).get("id").asLong()).isEqualTo(10);
        assertThat(body.get("hasNext").asBoolean()).isTrue();
        assertThat(body.get("hasPrev").asBoolean()).isFalse();
        assertThat(body.get("nextCursor").asText()).isNotEmpty();
    }

    @Test
    void sizeQueryParamIsClampedByAnnotationMaxSize() throws Exception {
        // Annotation says maxSize=20, but we ask for 9999.
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/test/events/keyset")
                        .queryParam("size", "9999")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.size").isEqualTo(20);
    }

    @Test
    void followingNextCursorAdvancesPages() throws Exception {
        EntityExchangeResult<byte[]> first = client.get()
                .uri("/test/events/keyset")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        JsonNode firstJson = objectMapper.readTree(first.getResponseBody());
        String nextCursor = firstJson.get("nextCursor").asText();
        long firstPageLastId = firstJson.get("content").get(2).get("id").asLong();

        EntityExchangeResult<byte[]> second = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/test/events/keyset")
                        .queryParam("cursor", nextCursor)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        JsonNode secondJson = objectMapper.readTree(second.getResponseBody());
        long secondPageFirstId = secondJson.get("content").get(0).get("id").asLong();

        // Second page must start strictly older than where the first ended.
        assertThat(secondPageFirstId).isLessThan(firstPageLastId);
        assertThat(secondJson.get("hasPrev").asBoolean()).isTrue();
    }

    @Test
    void garbageCursorFallsBackToFirstPage() throws Exception {
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/test/events/keyset")
                        .queryParam("cursor", "!!!not-a-real-token!!!")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].id").isEqualTo(10)   // first page = newest
                .jsonPath("$.hasPrev").isEqualTo(false);
    }

    @Test
    void prevCursorOnNonFirstPageRoundtrips() throws Exception {
        // Get a non-first page, then use its prevCursor to step back.
        EntityExchangeResult<byte[]> first = client.get()
                .uri("/test/events/keyset").exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        String nextCursor = objectMapper.readTree(first.getResponseBody())
                .get("nextCursor").asText();

        EntityExchangeResult<byte[]> second = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/test/events/keyset")
                        .queryParam("cursor", nextCursor)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        JsonNode secondJson = objectMapper.readTree(second.getResponseBody());
        String prevCursor = secondJson.get("prevCursor").asText();
        assertThat(prevCursor).isNotEmpty();

        EntityExchangeResult<byte[]> backToFirst = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/test/events/keyset")
                        .queryParam("cursor", prevCursor)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        JsonNode backJson = objectMapper.readTree(backToFirst.getResponseBody());

        // The "back" page should include the first page's newest item (id=10)
        // — we navigated from the second page back to the first.
        assertThat(backJson.get("content").get(0).get("id").asLong()).isEqualTo(10L);
    }
}
