package com.switchwon.api.dto;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 통화 1건의 최신 환율 응답.
 */
@Schema(description = "통화 1건의 최신 환율")
public record ExchangeRateResponse(
        @Schema(description = "통화 코드", example = "USD")
        Currency currency,

        @Schema(description = "전신환 매입율 (매매기준율 × 1.05)", example = "1551.32")
        BigDecimal buyRate,

        @Schema(description = "매매기준율", example = "1477.45")
        BigDecimal tradeStanRate,

        @Schema(description = "전신환 매도율 (매매기준율 × 0.95)", example = "1403.58")
        BigDecimal sellRate,

        @Schema(description = "환율 수집 시각", example = "2026-04-28T12:00:00")
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
