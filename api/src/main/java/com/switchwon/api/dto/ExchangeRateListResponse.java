package com.switchwon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 전체 통화 최신 환율 응답 래퍼.
 *
 * 과제 스펙:
 * <pre>
 * {
 *   "exchangeRateList": [ ... ]
 * }
 * </pre>
 */
@Schema(description = "전체 통화 최신 환율 응답")
public record ExchangeRateListResponse(
        @Schema(description = "통화별 최신 환율 목록 (USD/JPY/CNY/EUR)")
        List<ExchangeRateResponse> exchangeRateList
) {

    public static ExchangeRateListResponse of(List<ExchangeRateResponse> list) {
        return new ExchangeRateListResponse(list);
    }
}
