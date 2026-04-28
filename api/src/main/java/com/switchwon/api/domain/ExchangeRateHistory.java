package com.switchwon.api.domain;

import com.switchwon.api.exception.DomainErrorCode;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.common.util.MoneyCalculator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 수집 이력.
 * 통화별로 1분 단위 row가 누적되며, 가장 최신 row가 현재 환율의 정답이다.
 *
 * 인덱스: (currency, collected_at DESC)
 *  - "특정 통화 최신 환율" 조회를 O(log n)으로 수행
 *  - "당일 데이터 존재 여부" 조회 시에도 동작 (covering index)
 */
@Slf4j
@Getter
@Entity
@Table(
        name = "exchange_rate_history",
        indexes = @Index(
                name = "idx_currency_collected_at",
                columnList = "currency, collected_at DESC"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "trade_stan_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal tradeStanRate;

    @Column(name = "buy_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal buyRate;

    @Column(name = "sell_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellRate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    private ExchangeRateHistory(Currency currency,
                                BigDecimal tradeStanRate,
                                BigDecimal buyRate,
                                BigDecimal sellRate,
                                LocalDateTime collectedAt) {
        this.currency = currency;
        this.tradeStanRate = tradeStanRate;
        this.buyRate = buyRate;
        this.sellRate = sellRate;
        this.collectedAt = collectedAt;
    }

    /**
     * 매매기준율로부터 buyRate/sellRate를 자동 계산해 엔티티를 생성한다.
     * 산식과 정밀도는 모두 MoneyCalculator를 경유한다.
     */
    public static ExchangeRateHistory of(Currency currency,
                                         BigDecimal tradeStanRate,
                                         LocalDateTime collectedAt) {
        if (currency == null || currency == Currency.KRW) {
            log.warn("{} 환율 이력 생성 거부 사유=KRW거나_null currency={}",
                    LoggingPatterns.BIZ_FAIL, currency);
            throw new BusinessException(
                    DomainErrorCode.INVALID_RATE_TARGET,
                    "환율 이력은 외화에만 존재합니다. currency=" + currency);
        }
        BigDecimal rounded = MoneyCalculator.roundRate(tradeStanRate);
        BigDecimal buy = MoneyCalculator.buyRate(rounded);
        BigDecimal sell = MoneyCalculator.sellRate(rounded);
        log.info("{} 환율 이력 생성 currency={} tradeStanRate={} buyRate={} sellRate={} collectedAt={}",
                LoggingPatterns.BIZ_EVENT, currency, rounded, buy, sell, collectedAt);
        return new ExchangeRateHistory(currency, rounded, buy, sell, collectedAt);
    }
}
