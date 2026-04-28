package com.switchwon.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    @DisplayName("총_25건을_size_10으로_나누면_총_3페이지가_된다")
    void total_pages_is_calculated_from_total_elements_and_size() {
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 0, 10, 25L);

        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("마지막_페이지는_last_플래그가_true가_된다")
    void last_page_has_last_flag_true() {
        PageResponse<String> response = PageResponse.of(List.of("z"), 2, 10, 25L);

        assertThat(response.isLast()).isTrue();
        assertThat(response.isFirst()).isFalse();
    }

    @Test
    @DisplayName("결과가_없으면_totalPages_0이고_last는_true이다")
    void empty_result_has_total_pages_0_and_last_true() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 20, 0L);

        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("map으로_content를_변환하면_페이지_메타데이터는_그대로_유지된다")
    void map_preserves_page_metadata() {
        PageResponse<String> source = PageResponse.of(List.of("1", "22"), 0, 5, 10L);

        PageResponse<Integer> mapped = source.map(String::length);

        assertThat(mapped.getContent()).containsExactly(1, 2);
        assertThat(mapped.getPage()).isEqualTo(0);
        assertThat(mapped.getSize()).isEqualTo(5);
        assertThat(mapped.getTotalElements()).isEqualTo(10L);
    }
}
