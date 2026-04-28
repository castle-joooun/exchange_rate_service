package com.switchwon.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CommonErrorCodeTest {

    @Test
    @DisplayName("모든_CommonErrorCode는_code_message_httpStatus를_빠짐없이_가진다")
    void 모든_CommonErrorCode는_code_message_httpStatus를_빠짐없이_가진다() {
        assertThat(CommonErrorCode.values()).allSatisfy(errorCode -> {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getHttpStatus()).isNotNull();
        });
    }

    @Test
    @DisplayName("CommonErrorCode의_code는_COMMON_접두어를_가진다")
    void CommonErrorCode의_code는_COMMON_접두어를_가진다() {
        assertThat(CommonErrorCode.values())
                .extracting(CommonErrorCode::getCode)
                .allMatch(code -> code.startsWith("COMMON_"));
    }

    @Test
    @DisplayName("CommonErrorCode의_code는_서로_중복되지_않는다")
    void CommonErrorCode의_code는_서로_중복되지_않는다() {
        long distinct = Arrays.stream(CommonErrorCode.values())
                .map(CommonErrorCode::getCode)
                .collect(Collectors.toSet())
                .size();

        assertThat(distinct).isEqualTo(CommonErrorCode.values().length);
    }

    @Test
    @DisplayName("INVALID_DATA_INVALID_RATE는_BAD_REQUEST고_INTERNAL_ERROR는_500이다")
    void HttpStatus_매핑이_의도와_일치한다() {
        assertThat(CommonErrorCode.INVALID_DATA.getHttpStatus().is4xxClientError()).isTrue();
        assertThat(CommonErrorCode.INVALID_RATE.getHttpStatus().is4xxClientError()).isTrue();
        assertThat(CommonErrorCode.INTERNAL_ERROR.getHttpStatus().is5xxServerError()).isTrue();
    }
}
