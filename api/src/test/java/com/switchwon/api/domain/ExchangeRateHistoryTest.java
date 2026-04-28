package com.switchwon.api.domain;

import com.switchwon.api.exception.DomainErrorCode;
import com.switchwon.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateHistoryTest {

    @Test
    @DisplayName("매매기준율로부터_buyRate와_sellRate가_자동_계산되며_currency와_collectedAt도_그대로_저장된다")
    void 매매기준율로부터_buyRate와_sellRate가_자동_계산되며_currency와_collectedAt도_그대로_저장된다() {
        LocalDateTime collectedAt = LocalDateTime.of(2026, 4, 28, 11, 1);

        ExchangeRateHistory history = ExchangeRateHistory.of(
                Currency.USD,
                new BigDecimal("1477.45"),
                collectedAt
        );

        assertThat(history.getCurrency()).isEqualTo(Currency.USD);
        assertThat(history.getTradeStanRate()).isEqualByComparingTo("1477.45");
        assertThat(history.getBuyRate()).isEqualByComparingTo("1551.32");
        assertThat(history.getSellRate()).isEqualByComparingTo("1403.58");
        assertThat(history.getCollectedAt()).isEqualTo(collectedAt);
        assertThat(history.getId()).isNull();
    }

    @Test
    @DisplayName("매매기준율은_둘째_자리에서_반올림되고_buyRate_sellRate도_반올림된_값_기준으로_계산된다")
    void 매매기준율은_둘째_자리에서_반올림되고_buyRate_sellRate도_반올림된_값_기준으로_계산된다() {
        ExchangeRateHistory history = ExchangeRateHistory.of(
                Currency.USD,
                new BigDecimal("1477.455"),
                LocalDateTime.now()
        );

        assertThat(history.getTradeStanRate()).isEqualByComparingTo("1477.46");
        assertThat(history.getBuyRate()).isEqualByComparingTo("1551.33");
        assertThat(history.getSellRate()).isEqualByComparingTo("1403.59");
    }

    @Test
    @DisplayName("JPY_환율도_currency가_JPY로_저장되고_buy_sell이_올바르게_계산된다")
    void JPY_환율도_currency가_JPY로_저장되고_buy_sell이_올바르게_계산된다() {
        ExchangeRateHistory history = ExchangeRateHistory.of(
                Currency.JPY,
                new BigDecimal("910.50"),
                LocalDateTime.now()
        );

        assertThat(history.getCurrency()).isEqualTo(Currency.JPY);
        assertThat(history.getTradeStanRate()).isEqualByComparingTo("910.50");
        assertThat(history.getBuyRate()).isEqualByComparingTo("956.03");
        assertThat(history.getSellRate()).isEqualByComparingTo("864.98");
    }

    @Test
    @DisplayName("KRW_통화로_환율_이력을_생성하면_INVALID_RATE_TARGET_BusinessException이다")
    void KRW_통화로_환율_이력을_생성하면_INVALID_RATE_TARGET_BusinessException이다() {
        assertThatThrownBy(() -> ExchangeRateHistory.of(
                Currency.KRW, new BigDecimal("1"), LocalDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_RATE_TARGET);
    }

    @Test
    @DisplayName("null_통화로_환율_이력을_생성하면_INVALID_RATE_TARGET_BusinessException이다")
    void null_통화로_환율_이력을_생성하면_INVALID_RATE_TARGET_BusinessException이다() {
        assertThatThrownBy(() -> ExchangeRateHistory.of(
                null, new BigDecimal("1"), LocalDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_RATE_TARGET);
    }
}
