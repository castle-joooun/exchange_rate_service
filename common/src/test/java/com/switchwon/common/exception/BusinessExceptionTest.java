package com.switchwon.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;


class BusinessExceptionTest {

    @Test
    @DisplayName("BusinessException은_ErrorCode를_그대로_보유한다")
    void business_exception_holds_error_code() {
        ErrorCode errorCode = new SampleErrorCode("SAMPLE", "샘플 메시지", HttpStatus.BAD_REQUEST);

        BusinessException ex = new BusinessException(errorCode);

        assertThat(ex.getErrorCode()).isSameAs(errorCode);
        assertThat(ex.getMessage()).isEqualTo("샘플 메시지");
    }

    @Test
    @DisplayName("상세_메시지를_지정하면_ErrorCode_기본_메시지_대신_사용된다")
    void detail_message_overrides_default_error_code_message() {
        ErrorCode errorCode = new SampleErrorCode("SAMPLE", "기본", HttpStatus.BAD_REQUEST);

        BusinessException ex = new BusinessException(errorCode, "상세 사유");

        assertThat(ex.getMessage()).isEqualTo("상세 사유");
        assertThat(ex.getErrorCode().getMessage()).isEqualTo("기본");
    }

    private record SampleErrorCode(String code, String message, HttpStatus httpStatus) implements ErrorCode {
        @Override public String getCode() { return code; }
        @Override public String getMessage() { return message; }
        @Override public HttpStatus getHttpStatus() { return httpStatus; }
    }
}
