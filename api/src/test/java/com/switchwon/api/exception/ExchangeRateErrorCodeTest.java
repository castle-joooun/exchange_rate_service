package com.switchwon.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateErrorCodeTest {

    @Test
    @DisplayName("모든_ExchangeRateErrorCode는_code_message_httpStatus를_빠짐없이_가진다")
    void 모든_ExchangeRateErrorCode는_code_message_httpStatus를_빠짐없이_가진다() {
        assertThat(ExchangeRateErrorCode.values()).allSatisfy(errorCode -> {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getHttpStatus()).isNotNull();
        });
    }

    @Test
    @DisplayName("ExchangeRateErrorCode의_code는_RATE_접두어를_가진다")
    void ExchangeRateErrorCode의_code는_RATE_접두어를_가진다() {
        assertThat(ExchangeRateErrorCode.values())
                .extracting(ExchangeRateErrorCode::getCode)
                .allMatch(code -> code.startsWith("RATE_"));
    }

    @Test
    @DisplayName("ExchangeRateErrorCode의_code는_서로_중복되지_않는다")
    void ExchangeRateErrorCode의_code는_서로_중복되지_않는다() {
        long distinct = Arrays.stream(ExchangeRateErrorCode.values())
                .map(ExchangeRateErrorCode::getCode)
                .collect(Collectors.toSet())
                .size();

        assertThat(distinct).isEqualTo(ExchangeRateErrorCode.values().length);
    }

    @Test
    @DisplayName("EXCHANGE_RATE_NOT_FOUND는_404다")
    void EXCHANGE_RATE_NOT_FOUND는_404다() {
        assertThat(ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND.getHttpStatus().value())
                .isEqualTo(404);
    }
}
