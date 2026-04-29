package com.switchwon.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OrderErrorCodeTest {

    @Test
    @DisplayName("모든_OrderErrorCode는_code_message_httpStatus를_빠짐없이_가진다")
    void 모든_OrderErrorCode는_code_message_httpStatus를_빠짐없이_가진다() {
        assertThat(OrderErrorCode.values()).allSatisfy(errorCode -> {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getHttpStatus()).isNotNull();
        });
    }

    @Test
    @DisplayName("OrderErrorCode의_code는_ORDER_접두어를_가진다")
    void OrderErrorCode의_code는_ORDER_접두어를_가진다() {
        assertThat(OrderErrorCode.values())
                .extracting(OrderErrorCode::getCode)
                .allMatch(code -> code.startsWith("ORDER_"));
    }

    @Test
    @DisplayName("OrderErrorCode의_code는_서로_중복되지_않는다")
    void OrderErrorCode의_code는_서로_중복되지_않는다() {
        long distinct = Arrays.stream(OrderErrorCode.values())
                .map(OrderErrorCode::getCode)
                .collect(Collectors.toSet())
                .size();

        assertThat(distinct).isEqualTo(OrderErrorCode.values().length);
    }

    @Test
    @DisplayName("주문_검증_실패는_4xx로_매핑된다")
    void 주문_검증_실패는_4xx로_매핑된다() {
        assertThat(OrderErrorCode.values())
                .allSatisfy(ec -> assertThat(ec.getHttpStatus().is4xxClientError()).isTrue());
    }
}
