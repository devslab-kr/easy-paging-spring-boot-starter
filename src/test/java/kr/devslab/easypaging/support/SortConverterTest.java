package kr.devslab.easypaging.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.NullHandling;

class SortConverterTest {

    @Test
    void emptyForUnsortedOrNull() {
        assertThat(SortConverter.toOrderBy(Sort.unsorted())).isEmpty();
        assertThat(SortConverter.toOrderBy(null)).isEmpty();
    }

    @Test
    void singleAscOrder() {
        String orderBy = SortConverter.toOrderBy(Sort.by(Direction.ASC, "name"));
        assertThat(orderBy).isEqualTo("name asc");
    }

    @Test
    void multipleOrdersJoinedByComma() {
        Sort sort = Sort.by(
                Sort.Order.desc("created_at"),
                Sort.Order.asc("id"));
        assertThat(SortConverter.toOrderBy(sort)).isEqualTo("created_at desc, id asc");
    }

    @Test
    void nullHandlingPropagatesToFragment() {
        Sort sort = Sort.by(Sort.Order.desc("price").with(NullHandling.NULLS_LAST));
        assertThat(SortConverter.toOrderBy(sort)).isEqualTo("price desc nulls last");
    }

    @Test
    void dottedIdentifierAllowed() {
        Sort sort = Sort.by(Direction.ASC, "user.name");
        assertThat(SortConverter.toOrderBy(sort)).isEqualTo("user.name asc");
    }

    @Test
    void semicolonInjectionRejected() {
        Sort sort = Sort.by(Direction.ASC, "name; DROP TABLE users");
        assertThatThrownBy(() -> SortConverter.toOrderBy(sort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal characters");
    }

    @Test
    void spaceInPropertyRejected() {
        Sort sort = Sort.by(Direction.ASC, "name asc");
        assertThatThrownBy(() -> SortConverter.toOrderBy(sort))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parenthesisInjectionRejected() {
        Sort sort = Sort.by(Direction.ASC, "(SELECT 1)");
        assertThatThrownBy(() -> SortConverter.toOrderBy(sort))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
