package com.switchwon.api.seeder;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.service.ExchangeRateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션 부팅 시점에 환율 이력이 비어 있으면 기준 환율을 시드한다.
 *
 * <p>시드 우선순위:</p>
 * <ol>
 *   <li>외부 API (한국수출입은행) 호출 결과의 정상 매매기준율</li>
 *   <li>외부 API 호출 실패 또는 값이 falsy(null/0이하)인 통화는 하드코딩 fallback</li>
 * </ol>
 *
 * <p>이렇게 하는 이유: 첫 실행 직후 외부 호출 슬롯(평일 12/15/18/21시)이 아니거나
 * DB에 row 가 전혀 없으면 매분 스케줄러가 ±0.3% 변동을 시작할 base 가 없어 데드락이 발생한다.</p>
 *
 * <p>하드코딩 fallback (과제 시작 시점 현실적 수치):</p>
 * <ul>
 *   <li>USD: 1477.78</li>
 *   <li>JPY: 925.43 (100엔 단위)</li>
 *   <li>CNY: 216.10</li>
 *   <li>EUR: 1728.21</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialRateSeeder implements ApplicationRunner {

    /** 외부 API 호출 실패 또는 값이 비정상일 때 사용하는 하드코딩 환율. */
    private static final Map<Currency, BigDecimal> HARDCODED_FALLBACK = new LinkedHashMap<>();
    static {
        HARDCODED_FALLBACK.put(Currency.USD, new BigDecimal("1477.78"));
        HARDCODED_FALLBACK.put(Currency.JPY, new BigDecimal("925.43"));
        HARDCODED_FALLBACK.put(Currency.CNY, new BigDecimal("216.10"));
        HARDCODED_FALLBACK.put(Currency.EUR, new BigDecimal("1728.21"));
    }

    private final ExchangeRateHistoryRepository repository;
    private final ExchangeRateClient exchangeRateClient;
    private final Clock clock;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("{} 환율 이력이 이미 존재하여 시드를 생략합니다 count={}",
                    LoggingPatterns.BIZ_EVENT, repository.count());
            return;
        }

        Map<Currency, BigDecimal> external = tryFetchFromExternal();
        LocalDateTime now = LocalDateTime.now(clock);

        HARDCODED_FALLBACK.forEach((currency, fallbackRate) -> {
            BigDecimal externalRate = external.get(currency);
            BigDecimal seedRate;
            String source;
            if (isUsable(externalRate)) {
                seedRate = externalRate;
                source = "EXTERNAL";
            } else {
                seedRate = fallbackRate;
                source = "HARDCODED";
            }
            repository.save(ExchangeRateHistory.of(currency, seedRate, now));
            log.info("{} 기준 환율 시드 currency={} tradeStanRate={} source={}",
                    LoggingPatterns.BIZ_EVENT, currency, seedRate, source);
        });
    }

    /**
     * 외부 API 호출 시도. 실패 시 빈 Map 반환 (호출자가 하드코딩으로 fallback).
     */
    private Map<Currency, BigDecimal> tryFetchFromExternal() {
        try {
            List<DailyExchangeRate> rates = exchangeRateClient.fetchDailyRates(LocalDate.now(clock));
            Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
            for (DailyExchangeRate rate : rates) {
                try {
                    Currency currency = Currency.fromExternalCode(rate.currencyCode());
                    if (HARDCODED_FALLBACK.containsKey(currency) && isUsable(rate.tradeStanRate())) {
                        result.put(currency, rate.tradeStanRate());
                    }
                } catch (BusinessException e) {
                    // 우리가 다루지 않는 통화 (GBP, AUD 등) — 무시
                }
            }
            log.info("{} 외부 API 부팅 시드 매핑 결과 count={} currencies={}",
                    LoggingPatterns.BIZ_EVENT, result.size(), result.keySet());
            return result;
        } catch (Exception e) {
            log.warn("{} 부팅 시 외부 API 호출 실패. 4통화 모두 하드코딩 시드로 진행 reason={}",
                    LoggingPatterns.BIZ_FAIL, e.getClass().getSimpleName());
            return Map.of();
        }
    }

    /**
     * 시드용으로 사용 가능한 환율 값인지 검증. null 또는 0 이하면 falsy 로 간주.
     */
    private static boolean isUsable(BigDecimal rate) {
        return rate != null && rate.signum() > 0;
    }
}
