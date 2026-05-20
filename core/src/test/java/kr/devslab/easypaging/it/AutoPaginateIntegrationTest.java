package kr.devslab.easypaging.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.pagehelper.Page;
import java.util.List;
import kr.devslab.easypaging.core.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * End-to-end test: real H2 + MyBatis + PageHelper + our aspect.
 *
 * <p>Seed data has 12 rows. We verify slicing, total counts, count-off mode,
 * and sort propagation.
 */
@SpringBootTest(classes = TestApplication.class)
class AutoPaginateIntegrationTest {

    @Autowired
    private TestUserService service;

    @Test
    void firstPageReturnsCorrectSliceAndTotals() {
        PageResponse<TestUser> response = service.list(PageRequest.of(0, 5));

        assertThat(response.content()).hasSize(5);
        assertThat(response.content()).extracting(TestUser::getName)
                .containsExactly("alice", "bob", "charlie", "dave", "eve");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(12);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
    }

    @Test
    void middlePageReturnsCorrectSlice() {
        PageResponse<TestUser> response = service.list(PageRequest.of(1, 5));

        assertThat(response.content()).extracting(TestUser::getName)
                .containsExactly("frank", "grace", "heidi", "ivan", "judy");
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void lastPagePartiallyFilledAndMarkedLast() {
        PageResponse<TestUser> response = service.list(PageRequest.of(2, 5));

        assertThat(response.content()).hasSize(2);
        assertThat(response.last()).isTrue();
    }

    @Test
    void countDisabledReturnsUnknownTotal() {
        PageResponse<TestUser> response = service.listWithoutCount(PageRequest.of(0, 5));

        assertThat(response.content()).hasSize(5);
        assertThat(response.totalElements()).isLessThanOrEqualTo(0);
    }

    @Test
    void sortPropagatesToOrderBy() {
        Pageable sorted = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"));
        PageResponse<TestUser> response = service.list(sorted);

        // ids should be 12, 11, 10 — descending
        List<Long> ids = response.content().stream().map(TestUser::getId).toList();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    void requestedSizeIsClampedByAnnotationMax() {
        // Annotation says maxSize=50; we ask for 5000.
        PageResponse<TestUser> response = service.list(PageRequest.of(0, 5000));

        assertThat(response.size()).isLessThanOrEqualTo(50);
    }

    @Test
    void listReturnTypeIsPassedThroughWithoutWrapping() {
        // Methods declaring List return type should get the raw (PageHelper Page)
        // list back — sliced and ordered, but not wrapped in a PageResponse envelope.
        List<TestUser> list = service.listAsList(PageRequest.of(0, 4));

        assertThat(list).hasSize(4);
        assertThat(list).isInstanceOf(Page.class);
        Page<TestUser> page = (Page<TestUser>) list;
        assertThat(page.getTotal()).isEqualTo(12);
    }

    @Test
    void objectReturnTypeIsAutoWrappedByAspect() {
        // When declared return is Object, the aspect is free to wrap the List
        // result without violating any static type — useful for write-once
        // helper services that should still surface page metadata.
        Object result = service.listAsObject(PageRequest.of(1, 4));

        assertThat(result).isInstanceOf(PageResponse.class);
        @SuppressWarnings("unchecked")
        PageResponse<TestUser> response = (PageResponse<TestUser>) result;
        assertThat(response.content()).hasSize(4);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(12);
    }
}
