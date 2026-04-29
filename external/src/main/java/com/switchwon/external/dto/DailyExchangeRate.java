package com.switchwon.external.dto;

import java.math.BigDecimal;

/**
 * 외부 API 의 raw 응답에서 우리 시스템이 사용하는 두 값(통화 코드, 매매기준율)만 추려낸 record.
 * api 모듈은 KoreaeximRateResponse 가 아니라 이 record 만 의존한다.
 *
 * <p>주의: 통화 코드는 외부 API 가 응답한 형태를 그대로 보존한다.
 * 예: USD, JPY(100), CNH, EUR
 * 시스템 표준 통화(USD/JPY/CNY/EUR)로의 매핑은 api 모듈의 Currency.fromExternalCode 가 담당한다.
 * external 모듈이 도메인 통화를 알지 못하도록 책임을 명확히 분리한다.</p>
 *
 * <p>buyRate/sellRate 는 산식이 정해져 있으므로 이 DTO 에는 포함하지 않는다.
 * api 모듈의 ExchangeRateHistory.of() 가 매매기준율로부터 자동 계산한다.</p>
 */
public record DailyExchangeRate(
        String currencyCode,
        BigDecimal tradeStanRate
) {
}
