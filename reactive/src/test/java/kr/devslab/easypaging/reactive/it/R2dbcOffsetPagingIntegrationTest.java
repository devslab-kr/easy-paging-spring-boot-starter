package kr.devslab.easypaging.reactive.it;

import static org.assertj.core.api.Assertions.assertThat;

import kr.devslab.easypaging.core.PageResponse;
import kr.devslab.easypaging.r2dbc.R2dbcOffsetPagingSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import reactor.test.StepVerifier;

@SpringBootTest(classes = ReactiveTestApplication.class)
class R2dbcOffsetPagingIntegrationTest {

    @Autowired private R2dbcEntityTemplate template;

    @Test
    void firstPageReturnsRequestedSizeAndCorrectTotal() {
        StepVerifier.create(
                        R2dbcOffsetPagingSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                PageRequest.of(0, 3, Sort.by("id").ascending())))
                .assertNext((PageResponse<TestEvent> page) -> {
                    assertThat(page.content()).hasSize(3);
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(1L, 2L, 3L);
                    assertThat(page.totalElements()).isEqualTo(10L);
                    assertThat(page.totalPages()).isEqualTo(4);
                    assertThat(page.first()).isTrue();
                    assertThat(page.last()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void lastPageMayBeShorterThanSize() {
        // 10 rows, size 3 → pages 0,1,2 hold 3 rows; page 3 holds 1 (id=10).
        StepVerifier.create(
                        R2dbcOffsetPagingSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.empty(),
                                PageRequest.of(3, 3, Sort.by("id").ascending())))
                .assertNext(page -> {
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).getId()).isEqualTo(10L);
                    assertThat(page.totalElements()).isEqualTo(10L);
                    assertThat(page.last()).isTrue();
                    assertThat(page.first()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void criteriaFilterAffectsRowsAndTotal() {
        // Only ids 6..10 → 5 rows total. Page 0 size 3 → ids 6,7,8.
        StepVerifier.create(
                        R2dbcOffsetPagingSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.where("id").greaterThanOrEquals(6),
                                PageRequest.of(0, 3, Sort.by("id").ascending())))
                .assertNext(page -> {
                    assertThat(page.content()).extracting(TestEvent::getId).containsExactly(6L, 7L, 8L);
                    assertThat(page.totalElements()).isEqualTo(5L);  // filter applied to count too
                    assertThat(page.totalPages()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    void emptyResultProducesZeroTotal() {
        StepVerifier.create(
                        R2dbcOffsetPagingSupport.paginate(
                                template,
                                TestEvent.class,
                                Criteria.where("id").greaterThan(9999),
                                PageRequest.of(0, 10)))
                .assertNext(page -> {
                    assertThat(page.content()).isEmpty();
                    assertThat(page.totalElements()).isZero();
                    assertThat(page.totalPages()).isZero();
                    assertThat(page.empty()).isTrue();
                    assertThat(page.first()).isTrue();
                    assertThat(page.last()).isTrue();
                })
                .verifyComplete();
    }
}
