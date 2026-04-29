package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.Order;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GET /order/list 응답 한 건.
 *
 * <p>과제 스펙:</p>
 * <pre>
 * {
 *   "id": 1,
 *   "fromAmount": 296086,
 *   "fromCurrency": "KRW",
 *   "toAmount": 200.00,
 *   "toCurrency": "USD",
 *   "tradeRate": 1480.43,
 *   "dateTime": "2026-04-22T10:01:00"
 * }
 * </pre>
 */
@Schema(description = "주문 내역 한 건")
public record OrderListItemResponse(
        @Schema(description = "주문 PK", example = "1")
        Long id,

        @Schema(description = "출금 금액", example = "296086")
        BigDecimal fromAmount,

        @Schema(description = "출금 통화", example = "KRW")
        Currency fromCurrency,

        @Schema(description = "입금 금액", example = "200")
        BigDecimal toAmount,

        @Schema(description = "입금 통화", example = "USD")
        Currency toCurrency,

        @Schema(description = "주문 시점에 적용된 환율", example = "1480.43")
        BigDecimal tradeRate,

        @Schema(description = "주문 처리 시각", example = "2026-04-28T12:00:00")
        LocalDateTime dateTime
) {

    public static OrderListItemResponse from(Order order) {
        return new OrderListItemResponse(
                order.getId(),
                order.getFromAmount(),
                order.getFromCurrency(),
                order.getToAmount(),
                order.getToCurrency(),
                order.getTradeRate(),
                order.getCreatedAt()
        );
    }
}
