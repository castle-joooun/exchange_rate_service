package com.switchwon.external.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalErrorCodeTest {

    @Test
    @DisplayName("모든_ExternalErrorCode는_code_message_httpStatus를_빠짐없이_가진다")
    void 모든_ExternalErrorCode는_code_message_httpStatus를_빠짐없이_가진다() {
        assertThat(ExternalErrorCode.values()).allSatisfy(errorCode -> {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getHttpStatus()).isNotNull();
        });
    }

    @Test
    @DisplayName("ExternalErrorCode의_code는_EXTERNAL_접두어를_가진다")
    void ExternalErrorCode의_code는_EXTERNAL_접두어를_가진다() {
        assertThat(ExternalErrorCode.values())
                .extracting(ExternalErrorCode::getCode)
                .allMatch(code -> code.startsWith("EXTERNAL_"));
    }

    @Test
    @DisplayName("ExternalErrorCode의_code는_서로_중복되지_않는다")
    void ExternalErrorCode의_code는_서로_중복되지_않는다() {
        long distinct = Arrays.stream(ExternalErrorCode.values())
                .map(ExternalErrorCode::getCode)
                .collect(Collectors.toSet())
                .size();

        assertThat(distinct).isEqualTo(ExternalErrorCode.values().length);
    }

    @Test
    @DisplayName("외부_API_관련_오류는_모두_5xx_HttpStatus로_매핑된다")
    void 외부_API_관련_오류는_모두_5xx_HttpStatus로_매핑된다() {
        assertThat(ExternalErrorCode.values())
                .allSatisfy(errorCode ->
                        assertThat(errorCode.getHttpStatus().is5xxServerError()).isTrue());
    }
}
