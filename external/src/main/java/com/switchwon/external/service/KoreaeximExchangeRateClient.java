package com.switchwon.external.service;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.external.config.KoreaeximProperties;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.dto.KoreaeximRateResponse;
import com.switchwon.external.exception.ExternalErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 한국수출입은행 환율 API 호출 구현체.
 *
 * <p>책임 범위:</p>
 * <ul>
 *   <li>HTTP 호출 + 응답을 record 로 디시리얼라이즈</li>
 *   <li>응답 result 코드 검증 + 매매기준율 파싱</li>
 *   <li>Resilience4j 서킷브레이커 적용. OPEN 또는 호출 실패 시 ExchangeRateFallbackProvider 호출</li>
 * </ul>
 *
 * <p>책임 밖:</p>
 * <ul>
 *   <li>통화 코드 매핑 (JPY(100)→JPY, CNH→CNY) — api/domain Currency.fromExternalCode 가 담당</li>
 *   <li>지원 통화 필터링 — api 모듈에서 매핑 실패 시 스킵</li>
 *   <li>buy/sell rate 계산 — common/util MoneyCalculator 또는 api/domain ExchangeRateHistory.of 가 담당</li>
 * </ul>
 */
@Slf4j
@Component
public class KoreaeximExchangeRateClient implements ExchangeRateClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String API_PATH = "/site/program/financial/exchangeJSON";
    private static final String DATA_TYPE = "AP01";
    private static final String CIRCUIT_BREAKER_NAME = "koreaeximApi";

    private final RestClient restClient;
    private final KoreaeximProperties properties;
    private final ExchangeRateFallbackProvider fallbackProvider;

    public KoreaeximExchangeRateClient(
            @Qualifier("koreaeximRestClient") RestClient restClient,
            KoreaeximProperties properties,
            ExchangeRateFallbackProvider fallbackProvider) {
        this.restClient = restClient;
        this.properties = properties;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallback")
    public List<DailyExchangeRate> fetchDailyRates(LocalDate date) {
        long started = System.currentTimeMillis();
        String searchDate = date.format(DATE_FORMAT);

        log.info("{} 한국수출입은행 환율 API 호출 시작 searchDate={}", LoggingPatterns.BIZ_EVENT, searchDate);

        List<KoreaeximRateResponse> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(API_PATH)
                        .queryParam("authkey", properties.authKey())
                        .queryParam("searchdate", searchDate)
                        .queryParam("data", DATA_TYPE)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<KoreaeximRateResponse>>() {});

        long elapsed = System.currentTimeMillis() - started;

        if (raw == null || raw.isEmpty()) {
            log.info("{} 한국수출입은행 환율 API 응답 빈배열 searchDate={} elapsedMs={}",
                    LoggingPatterns.BIZ_EVENT, searchDate, elapsed);
            return Collections.emptyList();
        }

        List<DailyExchangeRate> mapped = raw.stream()
                .map(this::toDailyExchangeRate)
                .toList();

        log.info("{} 한국수출입은행 환율 API 호출 성공 searchDate={} count={} elapsedMs={}",
                LoggingPatterns.BIZ_EVENT, searchDate, mapped.size(), elapsed);

        return mapped;
    }

    /**
     * Circuit Breaker fallback. 시그니처는 원본 메서드와 동일 + 마지막에 Throwable.
     */
    @SuppressWarnings("unused")
    private List<DailyExchangeRate> fallback(LocalDate date, Throwable throwable) {
        log.warn("{} 외부 환율 API Fallback 동작 reason={} message={} date={}",
                LoggingPatterns.BIZ_FAIL,
                throwable.getClass().getSimpleName(),
                throwable.getMessage(),
                date);
        return fallbackProvider.getLatestAsRandomized();
    }

    /**
     * 외부 응답을 DailyExchangeRate 로 매핑한다.
     * - currencyCode 는 외부 응답 그대로 (예: "JPY(100)") 전달 — 매핑 책임은 api 모듈
     * - tradeStanRate 는 BigDecimal 파싱만 수행
     */
    private DailyExchangeRate toDailyExchangeRate(KoreaeximRateResponse response) {
        if (!response.isSuccess()) {
            log.warn("{} 환율 응답 result 비정상 cur_unit={} result={}",
                    LoggingPatterns.BIZ_FAIL, response.curUnit(), response.result());
            throw new BusinessException(
                    ExternalErrorCode.EXTERNAL_API_BAD_RESPONSE,
                    "한국수출입은행 응답 result 비정상: " + response.result());
        }
        BigDecimal rate = parseRate(response.dealBasRate(), response.curUnit());
        return new DailyExchangeRate(response.curUnit(), rate);
    }

    private static BigDecimal parseRate(String rate, String curUnit) {
        try {
            return new BigDecimal(rate.replace(",", "").trim());
        } catch (Exception e) {
            log.warn("{} 환율 값 파싱 실패 cur_unit={} value={} reason={}",
                    LoggingPatterns.BIZ_FAIL, curUnit, rate, e.getClass().getSimpleName());
            throw new BusinessException(
                    ExternalErrorCode.EXTERNAL_API_BAD_RESPONSE,
                    "환율 값 파싱 실패: cur_unit=" + curUnit + " value=" + rate);
        }
    }
}
