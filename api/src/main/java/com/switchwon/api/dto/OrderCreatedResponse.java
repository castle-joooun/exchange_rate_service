package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.Order;

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
public record OrderCreatedResponse(
        BigDecimal fromAmount,
        Currency fromCurrency,
        BigDecimal toAmount,
        Currency toCurrency,
        BigDecimal tradeRate,
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
