package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.service.ExchangeRateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 환율 수집 한 사이클의 컨트롤 타워.
 *
 * <ol>
 *   <li>RateCollectionPolicy 가 호출/Mock 결정</li>
 *   <li>CALL_API: external Client 호출 (실패 시 서킷브레이커 fallback 으로 자동 전환)</li>
 *   <li>MOCK_FROM_LATEST: LatestRateFallbackProvider 와 동일한 로직 활용</li>
 *   <li>결과를 통화별로 매핑하여 ExchangeRateHistory 로 저장</li>
 * </ol>
 *
 * <p>통화별 부분 실패 허용: 한 통화의 매핑/저장이 실패해도 다른 통화는 그대로 저장된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateCollector {

    private final RateCollectionPolicy policy;
    private final ExchangeRateClient exchangeRateClient;
    private final LatestRateFallbackProvider latestRateFallbackProvider;
    private final ExchangeRateHistoryRepository repository;
    private final Clock clock;

    /**
     * 트랜잭션은 메서드 단위가 아니라 통화별 단위가 자연스럽다.
     * JpaRepository.save 가 자체 트랜잭션을 갖고 있어, 한 통화의 저장 실패가 다른 통화에 영향을 주지 않는다.
     * (메서드에 @Transactional 을 걸면 한 건 실패 시 트랜잭션 전체가 mark-rollback 되어 부분 저장이 깨짐)
     */
    public void collectOnce() {
        long started = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now(clock);
        CollectionDecision decision = policy.decide();

        log.info("{} 환율 수집 사이클 시작 decision={} now={}", LoggingPatterns.BIZ_EVENT, decision, now);

        List<DailyExchangeRate> rates = switch (decision) {
            case CALL_API -> exchangeRateClient.fetchDailyRates(LocalDate.now(clock));
            case MOCK_FROM_LATEST -> latestRateFallbackProvider.getLatestAsRandomized();
        };

        if (rates.isEmpty()) {
            log.warn("{} 환율 수집 결과가 비어 있어 저장을 생략합니다 decision={} elapsedMs={}",
                    LoggingPatterns.BIZ_FAIL, decision, System.currentTimeMillis() - started);
            return;
        }

        List<String> savedCurrencies = new ArrayList<>();
        List<String> skippedCurrencies = new ArrayList<>();

        for (DailyExchangeRate rate : rates) {
            String currencyCode = rate.currencyCode();
            try {
                Currency currency = Currency.fromExternalCode(currencyCode);
                ExchangeRateHistory history = ExchangeRateHistory.of(currency, rate.tradeStanRate(), now);
                repository.save(history);
                savedCurrencies.add(currencyCode);
            } catch (BusinessException e) {
                // 미지원 통화 등 — 해당 통화만 스킵
                log.warn("{} 환율 이력 저장 스킵 currencyCode={} reason={}",
                        LoggingPatterns.BIZ_FAIL, currencyCode, e.getErrorCode().getCode());
                skippedCurrencies.add(currencyCode);
            } catch (Exception e) {
                log.error("{} 환율 이력 저장 실패 currencyCode={} reason={}",
                        LoggingPatterns.SYS_ERROR, currencyCode, e.getClass().getSimpleName(), e);
                skippedCurrencies.add(currencyCode);
            }
        }

        long elapsed = System.currentTimeMillis() - started;
        log.info("{} 환율 수집 사이클 완료 savedCount={} savedCurrencies={} skippedCount={} skippedCurrencies={} elapsedMs={} decision={}",
                LoggingPatterns.BIZ_EVENT,
                savedCurrencies.size(), savedCurrencies,
                skippedCurrencies.size(), skippedCurrencies,
                elapsed, decision);

        if (savedCurrencies.isEmpty() && !skippedCurrencies.isEmpty()) {
            log.error("{} 환율 수집 사이클 전부 실패 skippedCurrencies={} decision={}",
                    LoggingPatterns.SYS_ERROR, skippedCurrencies, decision);
        }
    }
}
