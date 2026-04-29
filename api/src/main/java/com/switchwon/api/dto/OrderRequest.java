package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 외화 주문 요청.
 *
 * <p>과제 스펙:</p>
 * <pre>
 * {
 *   "forexAmount": 200,
 *   "fromCurrency": "KRW",
 *   "toCurrency": "USD"
 * }
 * </pre>
 *
 * <ul>
 *   <li>forexAmount 는 외화 기준</li>
 *   <li>fromCurrency / toCurrency 중 하나는 KRW 여야 함 (검증은 OrderType.resolve)</li>
 *   <li>JPY 의 경우 100엔 미만 입력은 OrderService 에서 거부</li>
 * </ul>
 */
public record OrderRequest(
        @NotNull(message = "forexAmount는 필수입니다")
        @Positive(message = "forexAmount는 0보다 커야 합니다")
        BigDecimal forexAmount,

        @NotNull(message = "fromCurrency는 필수입니다")
        Currency fromCurrency,

        @NotNull(message = "toCurrency는 필수입니다")
        Currency toCurrency
) {
}
