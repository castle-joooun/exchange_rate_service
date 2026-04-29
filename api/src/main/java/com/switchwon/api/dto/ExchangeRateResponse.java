package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 통화 1건의 최신 환율 응답.
 *
 * 과제 스펙:
 * <pre>
 * {
 *   "currency": "USD",
 *   "buyRate": 1480.43,
 *   "tradeStanRate": 1477.45,
 *   "sellRate": 1474.47,
 *   "dateTime": "2026-04-22T10:01:00"
 * }
 * </pre>
 */
public record ExchangeRateResponse(
        Currency currency,
        BigDecimal buyRate,
        BigDecimal tradeStanRate,
        BigDecimal sellRate,
        LocalDateTime dateTime
) {

    public static ExchangeRateResponse from(ExchangeRateHistory history) {
        return new ExchangeRateResponse(
                history.getCurrency(),
                history.getBuyRate(),
                history.getTradeStanRate(),
                history.getSellRate(),
                history.getCollectedAt()
        );
    }
}
