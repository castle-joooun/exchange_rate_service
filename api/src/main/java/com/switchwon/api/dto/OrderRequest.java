package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "외화 매수/매도 주문 요청")
public record OrderRequest(
        @Schema(description = "외화 기준 주문 금액. JPY 는 100엔 이상 필수", example = "200")
        @NotNull(message = "forexAmount는 필수입니다")
        @Positive(message = "forexAmount는 0보다 커야 합니다")
        BigDecimal forexAmount,

        @Schema(description = "출금 통화 (매수 시 KRW)", example = "KRW")
        @NotNull(message = "fromCurrency는 필수입니다")
        Currency fromCurrency,

        @Schema(description = "입금 통화 (매수 시 외화)", example = "USD")
        @NotNull(message = "toCurrency는 필수입니다")
        Currency toCurrency
) {
}
