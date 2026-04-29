package com.switchwon.api.dto;

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
public record ExchangeRateListResponse(
        List<ExchangeRateResponse> exchangeRateList
) {

    public static ExchangeRateListResponse of(List<ExchangeRateResponse> list) {
        return new ExchangeRateListResponse(list);
    }
}
