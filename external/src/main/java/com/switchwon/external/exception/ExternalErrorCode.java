package com.switchwon.external.exception;

import com.switchwon.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExternalErrorCode implements ErrorCode {

    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_001", "외부 환율 API를 사용할 수 없습니다"),
    EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "EXTERNAL_002", "외부 환율 API 응답이 지연되고 있습니다"),
    EXTERNAL_API_BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "EXTERNAL_003", "외부 환율 API 응답을 해석할 수 없습니다"),
    EXTERNAL_API_AUTH_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_004", "외부 환율 API 인증에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override public String getMessage() { return message; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public String getCode() { return code; }
}
