package com.switchwon.api.exception;

import com.switchwon.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExchangeRateErrorCode implements ErrorCode {

    EXCHANGE_RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "RATE_001", "해당 통화의 최신 환율 정보를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override public String getMessage() { return message; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public String getCode() { return code; }
}
