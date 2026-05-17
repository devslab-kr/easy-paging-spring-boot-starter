package kr.devslab.easypaging.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.pagehelper.Page;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class PageResponseTest {

    @Test
    void fromPlainListUsesUnknownTotal() {
        List<String> list = List.of("a", "b", "c");
        Pageable pageable = PageRequest.of(0, 10);

        PageResponse<String> response = PageResponse.from(list, pageable);

        assertThat(response.content()).containsExactly("a", "b", "c");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(PageResponse.UNKNOWN_TOTAL);
        assertThat(response.totalPages()).isEqualTo(-1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue(); // 3 < 10 → last page
    }

    @Test
    void fromPageHelperPageExtractsTotals() {
        Page<String> page = new Page<>(1, 5);  // pageNum=1, pageSize=5
        page.setTotal(12);
        page.addAll(List.of("a", "b", "c", "d", "e"));

        PageResponse<String> response = PageResponse.from(page, PageRequest.of(0, 5));

        assertThat(response.content()).hasSize(5);
        assertThat(response.totalElements()).isEqualTo(12);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
    }

    @Test
    void empty() {
        PageResponse<String> response = PageResponse.empty(PageRequest.of(2, 20));
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();
        assertThat(response.empty()).isTrue();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void contentIsImmutable() {
        java.util.ArrayList<String> mutable = new java.util.ArrayList<>(List.of("a"));
        PageResponse<String> response = PageResponse.from(mutable, PageRequest.of(0, 10));
        mutable.add("b"); // must not affect the response

        assertThat(response.content()).containsExactly("a");
    }

    @Test
    void withOneIndexedPagesShiftsPageByOne() {
        Page<String> page = new Page<>(1, 5);
        page.setTotal(12);
        page.addAll(List.of("a", "b", "c", "d", "e"));

        PageResponse<String> zeroBased = PageResponse.from(page, PageRequest.of(0, 5));
        PageResponse<String> oneBased = zeroBased.withOneIndexedPages();

        // Only the page index shifts — other metadata is identical.
        assertThat(zeroBased.page()).isZero();
        assertThat(oneBased.page()).isEqualTo(1);
        assertThat(oneBased.content()).isEqualTo(zeroBased.content());
        assertThat(oneBased.size()).isEqualTo(zeroBased.size());
        assertThat(oneBased.totalElements()).isEqualTo(zeroBased.totalElements());
        assertThat(oneBased.totalPages()).isEqualTo(zeroBased.totalPages());
        assertThat(oneBased.first()).isEqualTo(zeroBased.first());
        assertThat(oneBased.last()).isEqualTo(zeroBased.last());
        assertThat(oneBased.empty()).isEqualTo(zeroBased.empty());
    }

    @Test
    void withOneIndexedPagesIsIdempotentForCallerControl() {
        // Calling twice shifts by 2 — withOneIndexedPages is a pure transform,
        // not a "ensure 1-based" guard. Caller is responsible for applying it
        // exactly once. (Documenting the behavior here so it doesn't change
        // silently.)
        PageResponse<String> base = PageResponse.from(List.of("x"), PageRequest.of(0, 5));
        assertThat(base.withOneIndexedPages().withOneIndexedPages().page()).isEqualTo(2);
    }
}
