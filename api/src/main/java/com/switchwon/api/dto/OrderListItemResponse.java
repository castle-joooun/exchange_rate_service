package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.Order;

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
public record OrderListItemResponse(
        Long id,
        BigDecimal fromAmount,
        Currency fromCurrency,
        BigDecimal toAmount,
        Currency toCurrency,
        BigDecimal tradeRate,
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
