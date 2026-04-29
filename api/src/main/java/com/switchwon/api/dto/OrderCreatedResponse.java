package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.Order;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POST /order 응답.
 *
 * <p>과제 스펙:</p>
 * <pre>
 * {
 *   "fromAmount": 296086,
 *   "fromCurrency": "KRW",
 *   "toAmount": 200.00,
 *   "toCurrency": "USD",
 *   "tradeRate": 1480.43,
 *   "dateTime": "2026-04-22T10:01:00"
 * }
 * </pre>
 *
 * <p>id 는 노출하지 않는다 (스펙 준수).
 * GET /order/list 응답에는 id 가 포함되며, {@link OrderListItemResponse} 가 담당한다.</p>
 */
@Schema(description = "주문 생성 결과")
public record OrderCreatedResponse(
        @Schema(description = "출금 금액", example = "296086")
        BigDecimal fromAmount,

        @Schema(description = "출금 통화", example = "KRW")
        Currency fromCurrency,

        @Schema(description = "입금 금액", example = "200")
        BigDecimal toAmount,

        @Schema(description = "입금 통화", example = "USD")
        Currency toCurrency,

        @Schema(description = "적용 환율 (매수=buyRate, 매도=sellRate)", example = "1480.43")
        BigDecimal tradeRate,

        @Schema(description = "주문 처리 시각", example = "2026-04-28T12:00:00")
        LocalDateTime dateTime
) {

    public static OrderCreatedResponse from(Order order) {
        return new OrderCreatedResponse(
                order.getFromAmount(),
                order.getFromCurrency(),
                order.getToAmount(),
                order.getToCurrency(),
                order.getTradeRate(),
                order.getCreatedAt()
        );
    }
}
