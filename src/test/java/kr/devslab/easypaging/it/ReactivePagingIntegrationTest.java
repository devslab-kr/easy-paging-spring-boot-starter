package kr.devslab.easypaging.it;

import static org.assertj.core.api.Assertions.assertThat;

import kr.devslab.easypaging.reactive.ReactivePagingSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import reactor.test.StepVerifier;

@SpringBootTest(classes = TestApplication.class)
class ReactivePagingIntegrationTest {

    @Autowired
    private TestUserMapper mapper;

    @Test
    void paginateOverReactor() {
        StepVerifier.create(ReactivePagingSupport.paginate(
                        PageRequest.of(0, 5),
                        mapper::findAll,
                        50,
                        true))
                .assertNext(response -> {
                    assertThat(response.content()).hasSize(5);
                    assertThat(response.totalElements()).isEqualTo(12);
                    assertThat(response.totalPages()).isEqualTo(3);
                    assertThat(response.first()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void paginateHonorsMaxSize() {
        StepVerifier.create(ReactivePagingSupport.paginate(
                        PageRequest.of(0, 9999),  // caller asks for a huge page
                        mapper::findAll,
                        7,                          // but we cap at 7
                        true))
                .assertNext(response -> assertThat(response.size()).isEqualTo(7))
                .verifyComplete();
    }
}
