package kr.devslab.easypaging.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import kr.devslab.easypaging.spi.PageResponseFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/**
 * Verifies that registering a {@link PageResponseFactory} bean replaces the
 * default {@link kr.devslab.easypaging.core.PageResponse} envelope. The factory
 * only fires when the aspect itself wraps the result — i.e. when the
 * controller declares an {@code Object} (or {@code PageResponse}) return type.
 */
@SpringBootTest(classes = TestApplication.class)
@Import(PageResponseFactoryIntegrationTest.CompanyEnvelopeConfig.class)
class PageResponseFactoryIntegrationTest {

    @Autowired
    private TestUserService service;

    @Test
    void factoryOverridesEnvelopeForObjectReturn() {
        Object result = service.listAsObject(PageRequest.of(0, 5));

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) result;

        // Shape produced by CompanyEnvelopeConfig#companyEnvelope
        assertThat(envelope).containsEntry("ok", true);
        assertThat(envelope).containsEntry("page", 0);
        assertThat(envelope).containsEntry("size", 5);
        assertThat(envelope).containsEntry("total", 12L);
        assertThat(envelope.get("data")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<TestUser> data = (List<TestUser>) envelope.get("data");
        assertThat(data).hasSize(5);
        assertThat(data).extracting(TestUser::getName)
                .containsExactly("alice", "bob", "charlie", "dave", "eve");
    }

    @TestConfiguration
    static class CompanyEnvelopeConfig {
        @Bean
        PageResponseFactory companyEnvelope() {
            return (content, pageable, totalElements, totalPages) -> Map.of(
                    "ok", true,
                    "data", content,
                    "page", pageable.getPageNumber(),
                    "size", pageable.getPageSize(),
                    "total", totalElements,
                    "pages", totalPages);
        }
    }
}
