package com.switchwon.api.seeder;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.common.log.LoggingPatterns;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 애플리케이션 부팅 시점에 환율 이력이 비어 있으면 기준 환율을 시드한다.
 *
 * 첫 시작 직후 12시 이전이라 외부 API 호출도 안 되고 DB도 비어있어 Mock 변동도 못 만드는
 * "환율 데드락"을 방지하기 위함이다. 이 시드 row 가 있으면 매분 스케줄러가 ±0.3% 변동을
 * 시작할 수 있다.
 *
 * <p>기준 환율 (과제 시작 시점 현실적 수치):</p>
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

    private static final Map<Currency, BigDecimal> SEED = new LinkedHashMap<>();
    static {
        SEED.put(Currency.USD, new BigDecimal("1477.78"));
        SEED.put(Currency.JPY, new BigDecimal("925.43"));
        SEED.put(Currency.CNY, new BigDecimal("216.10"));
        SEED.put(Currency.EUR, new BigDecimal("1728.21"));
    }

    private final ExchangeRateHistoryRepository repository;
    private final Clock clock;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("{} 환율 이력이 이미 존재하여 시드를 생략합니다 count={}",
                    LoggingPatterns.BIZ_EVENT, repository.count());
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        SEED.forEach((currency, rate) -> {
            repository.save(ExchangeRateHistory.of(currency, rate, now));
            log.info("{} 기준 환율 시드 currency={} tradeStanRate={}",
                    LoggingPatterns.BIZ_EVENT, currency, rate);
        });
    }
}
