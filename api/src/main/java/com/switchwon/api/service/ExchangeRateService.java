package com.switchwon.api.service;

import com.switchwon.api.config.CacheConfig;
import com.switchwon.api.domain.Currency;
import com.switchwon.api.dto.ExchangeRateListResponse;
import com.switchwon.api.dto.ExchangeRateResponse;
import com.switchwon.api.exception.ExchangeRateErrorCode;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 환율 조회 서비스.
 *
 * <p>캐시: {@link CacheConfig#LATEST_EXCHANGE_RATES} TTL 60초.
 * 분당 1회 새 row 가 들어오므로 60초 stale 은 충분히 허용 가능.</p>
 *
 * <p>외화 4종(USD/JPY/CNY/EUR) 만 조회 대상. KRW 는 기준 통화라 환율이 없음.</p>
 *
 * <p>캐시 키 전략: 같은 캐시에 들어가는 키 충돌을 막기 위해 prefix 를 명시한다.</p>
 * <ul>
 *   <li>전체 조회: {@code "ALL"}</li>
 *   <li>단건 조회: {@code "CURRENCY:USD"} 등</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final List<Currency> SUPPORTED_FOREX = List.of(
            Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR);

    private final ExchangeRateHistoryRepository repository;

    /**
     * 4개 통화 최신 환율 일괄 조회.
     */
    @Cacheable(value = CacheConfig.LATEST_EXCHANGE_RATES, key = "'ALL'")
    public ExchangeRateListResponse getLatestAll() {
        log.info("{} 전체 통화 환율 조회 (캐시 미스)", LoggingPatterns.BIZ_EVENT);

        List<ExchangeRateResponse> list = SUPPORTED_FOREX.stream()
                .map(repository::findTopByCurrencyOrderByCollectedAtDesc)
                .flatMap(Optional::stream)
                .map(ExchangeRateResponse::from)
                .toList();

        return ExchangeRateListResponse.of(list);
    }

    /**
     * 단일 통화 최신 환율 조회.
     * 데이터가 없으면 EXCHANGE_RATE_NOT_FOUND.
     */
    @Cacheable(value = CacheConfig.LATEST_EXCHANGE_RATES, key = "'CURRENCY:' + #currency.name()")
    public ExchangeRateResponse getLatest(Currency currency) {
        log.info("{} 단일 통화 환율 조회 (캐시 미스) currency={}", LoggingPatterns.BIZ_EVENT, currency);

        return repository.findTopByCurrencyOrderByCollectedAtDesc(currency)
                .map(ExchangeRateResponse::from)
                .orElseThrow(() -> {
                    log.warn("{} 환율 정보 없음 currency={}", LoggingPatterns.BIZ_FAIL, currency);
                    return new BusinessException(
                            ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND,
                            "환율 정보 없음: currency=" + currency);
                });
    }
}
