package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.service.ExchangeRateFallbackProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * external 모듈의 ExchangeRateFallbackProvider 구현체.
 *
 * 외부 API 호출이 실패(서킷 OPEN, 4xx/5xx, 타임아웃)했을 때 호출되며,
 * DB의 가장 최근 row 를 base 로 ±0.3% 랜덤 변동된 값을 반환한다.
 *
 * 의존성 방향: external 은 인터페이스만 정의, 구현은 api 모듈이 함 (헥사고날 스타일).
 *
 * 반환 시 currencyCode 는 외부 응답 형식과 동일하게 맞춘다 (예: JPY → "JPY(100)", CNY → "CNH").
 * 그래야 후단의 처리 로직이 정상/Fallback 결과를 동일하게 다룰 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LatestRateFallbackProvider implements ExchangeRateFallbackProvider {

    private final ExchangeRateHistoryRepository repository;
    private final MockRateGenerator mockRateGenerator;

    @Override
    public List<DailyExchangeRate> getLatestAsRandomized() {
        List<DailyExchangeRate> result = new ArrayList<>();

        for (Currency currency : Arrays.asList(Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR)) {
            repository.findTopByCurrencyOrderByCollectedAtDesc(currency)
                    .ifPresentOrElse(latest -> {
                        var nextRate = mockRateGenerator.generate(latest.getTradeStanRate());
                        result.add(new DailyExchangeRate(toExternalCode(currency), nextRate));
                    }, () -> log.warn("{} Fallback 실패: DB에 {} 통화의 환율 row가 없습니다",
                            LoggingPatterns.BIZ_FAIL, currency));
        }

        log.info("{} Fallback 환율 생성 count={}", LoggingPatterns.BIZ_EVENT, result.size());
        return result;
    }

    /**
     * 시스템 통화 → 외부 API 표기 변환 (역매핑).
     */
    private static String toExternalCode(Currency currency) {
        return switch (currency) {
            case JPY -> "JPY(100)";
            case CNY -> "CNH";
            default -> currency.name();
        };
    }
}
