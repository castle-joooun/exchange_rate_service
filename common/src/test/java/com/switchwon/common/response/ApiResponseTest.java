package com.switchwon.common.response;

import com.switchwon.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("성공_응답은_OK_코드와_데이터를_가진다")
    void success_response_has_ok_code_and_data() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.getCode()).isEqualTo("OK");
        assertThat(response.getMessage()).isEqualTo("SUCCESS");
        assertThat(response.getReturnObject()).isEqualTo("hello");
    }

    @Test
    @DisplayName("MDC에_등록된_traceId가_응답에_포함된다")
    void traceId_from_mdc_is_included_in_response() {
        MDC.put(ApiResponse.MDC_TRACE_ID, "abc123");

        ApiResponse<String> response = ApiResponse.success("x");

        assertThat(response.getTraceId()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("ErrorCode로_에러_응답을_생성하면_코드와_메시지가_매핑된다")
    void error_response_maps_code_and_message_from_error_code() {
        ErrorCode errorCode = new TestErrorCode("EXCHANGE_RATE_NOT_FOUND", "환율 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND);

        ApiResponse<Void> response = ApiResponse.error(errorCode);

        assertThat(response.getCode()).isEqualTo("EXCHANGE_RATE_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("환율 정보를 찾을 수 없습니다");
        assertThat(response.getReturnObject()).isNull();
    }

    private record TestErrorCode(String code, String message, HttpStatus httpStatus) implements ErrorCode {
        @Override public String getCode() { return code; }
        @Override public String getMessage() { return message; }
        @Override public HttpStatus getHttpStatus() { return httpStatus; }
    }
}
