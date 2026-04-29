package com.switchwon.api.exception;

import com.switchwon.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    /**
     * JPY 환율은 100엔 단위로 저장되므로, 100엔 미만 주문은 환산 시 0 KRW 가 되어 의미가 없다.
     */
    JPY_AMOUNT_BELOW_MIN_UNIT(HttpStatus.BAD_REQUEST, "ORDER_001", "JPY 주문은 최소 100엔 이상이어야 합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override public String getMessage() { return message; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public String getCode() { return code; }
}
