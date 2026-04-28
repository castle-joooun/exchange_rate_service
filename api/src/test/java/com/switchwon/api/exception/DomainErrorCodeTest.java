package com.switchwon.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DomainErrorCodeTest {

    @Test
    @DisplayName("모든_DomainErrorCode는_code_message_httpStatus를_빠짐없이_가진다")
    void 모든_DomainErrorCode는_code_message_httpStatus를_빠짐없이_가진다() {
        assertThat(DomainErrorCode.values()).allSatisfy(errorCode -> {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getHttpStatus()).isNotNull();
        });
    }

    @Test
    @DisplayName("DomainErrorCode의_code는_DOMAIN_접두어를_가진다")
    void DomainErrorCode의_code는_DOMAIN_접두어를_가진다() {
        assertThat(DomainErrorCode.values())
                .extracting(DomainErrorCode::getCode)
                .allMatch(code -> code.startsWith("DOMAIN_"));
    }

    @Test
    @DisplayName("DomainErrorCode의_code는_서로_중복되지_않는다")
    void DomainErrorCode의_code는_서로_중복되지_않는다() {
        long distinct = Arrays.stream(DomainErrorCode.values())
                .map(DomainErrorCode::getCode)
                .collect(Collectors.toSet())
                .size();

        assertThat(distinct).isEqualTo(DomainErrorCode.values().length);
    }

    @Test
    @DisplayName("도메인_검증_실패는_모두_4xx_클라이언트_오류로_매핑된다")
    void 도메인_검증_실패는_모두_4xx_클라이언트_오류로_매핑된다() {
        assertThat(DomainErrorCode.values())
                .allSatisfy(errorCode ->
                        assertThat(errorCode.getHttpStatus().is4xxClientError()).isTrue());
    }

    @Test
    @DisplayName("UNSUPPORTED_CURRENCY는_DOMAIN_001이다")
    void UNSUPPORTED_CURRENCY는_DOMAIN_001이다() {
        assertThat(DomainErrorCode.UNSUPPORTED_CURRENCY.getCode()).isEqualTo("DOMAIN_001");
    }

    @Test
    @DisplayName("INVALID_CURRENCY_PAIR는_DOMAIN_002이다")
    void INVALID_CURRENCY_PAIR는_DOMAIN_002이다() {
        assertThat(DomainErrorCode.INVALID_CURRENCY_PAIR.getCode()).isEqualTo("DOMAIN_002");
    }
}
