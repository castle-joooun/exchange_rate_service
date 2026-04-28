package com.switchwon.api.exception;

import com.switchwon.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DomainErrorCode implements ErrorCode {

    UNSUPPORTED_CURRENCY(HttpStatus.BAD_REQUEST, "DOMAIN_001", "지원하지 않는 통화입니다"),
    INVALID_CURRENCY_PAIR(HttpStatus.BAD_REQUEST, "DOMAIN_002", "주문은 KRW와 외화 사이에서만 가능합니다"),
    INVALID_RATE_TARGET(HttpStatus.BAD_REQUEST, "DOMAIN_003", "환율 이력은 외화에만 존재합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override public String getMessage() { return message; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public String getCode() { return code; }
}
