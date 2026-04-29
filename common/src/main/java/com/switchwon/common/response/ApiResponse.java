package com.switchwon.common.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.switchwon.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.MDC;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class ApiResponse<T> {

    public static final String SUCCESS_CODE = "OK";
    public static final String SUCCESS_MESSAGE = "SUCCESS";
    public static final String MDC_TRACE_ID = "traceId";

    private final String code;
    private final String message;

    /**
     * 응답 본문에는 노출하지 않는다.
     * traceId 는 로그/MDC 추적과 응답 헤더(X-Trace-Id, MdcFilter 가 자동 셋팅)에서만 사용한다.
     * getter 는 살아있어 테스트/디버깅 용도로 접근 가능.
     */
    @JsonIgnore
    private final String traceId;

    private final T returnObject;

    public static <T> ApiResponse<T> success(T returnObject) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, currentTraceId(), returnObject);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, currentTraceId(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), currentTraceId(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String detailMessage) {
        return new ApiResponse<>(errorCode.getCode(), detailMessage, currentTraceId(), null);
    }

    private static String currentTraceId() {
        return MDC.get(MDC_TRACE_ID);
    }
}
