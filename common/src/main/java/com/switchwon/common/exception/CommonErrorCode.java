package com.switchwon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_DATA(HttpStatus.BAD_REQUEST, "COMMON_001", "입력 값이 올바르지 않습니다"),
    INVALID_RATE(HttpStatus.BAD_REQUEST, "COMMON_002", "환율 값이 올바르지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override public String getMessage() { return message; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public String getCode() { return code; }
}
