package com.switchwon.common.util;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyCalculatorTest {

    @Test
    @DisplayName("환율은_소수점_둘째_자리에서_HALF_UP으로_반올림된다")
    void 환율은_소수점_둘째_자리에서_HALF_UP으로_반올림된다() {
        BigDecimal rounded = MoneyCalculator.roundRate(new BigDecimal("1477.455"));

        assertThat(rounded).isEqualByComparingTo("1477.46");
    }

    @Test
    @DisplayName("환율_반올림에서_5미만은_내림된다")
    void 환율_반올림에서_5미만은_내림된다() {
        BigDecimal rounded = MoneyCalculator.roundRate(new BigDecimal("1477.454"));

        assertThat(rounded).isEqualByComparingTo("1477.45");
    }

    @Test
    @DisplayName("매매기준율_1477_45에_5퍼센트_가산하면_buyRate는_1551_32이다")
    void 매매기준율_1477_45에_5퍼센트_가산하면_buyRate는_1551_32이다() {
        BigDecimal buyRate = MoneyCalculator.buyRate(new BigDecimal("1477.45"));

        assertThat(buyRate).isEqualByComparingTo("1551.32");
    }

    @Test
    @DisplayName("매매기준율_1477_45에_5퍼센트_차감하면_sellRate는_1403_58이다")
    void 매매기준율_1477_45에_5퍼센트_차감하면_sellRate는_1403_58이다() {
        BigDecimal sellRate = MoneyCalculator.sellRate(new BigDecimal("1477.45"));

        assertThat(sellRate).isEqualByComparingTo("1403.58");
    }

    @Test
    @DisplayName("JPY_1엔_단위_9_105를_100엔_단위로_환산하면_910_50이다")
    void JPY_1엔_단위_9_105를_100엔_단위로_환산하면_910_50이다() {
        BigDecimal jpy100 = MoneyCalculator.toJpyHundredUnit(new BigDecimal("9.105"));

        assertThat(jpy100).isEqualByComparingTo("910.50");
    }

    @Test
    @DisplayName("USD_200을_buyRate_1480_43으로_KRW로_환산하면_소수점_이하_버림되어_296086이다")
    void USD_200을_buyRate_1480_43으로_KRW로_환산하면_소수점_이하_버림되어_296086이다() {
        BigDecimal krw = MoneyCalculator.toKrw(new BigDecimal("200"), new BigDecimal("1480.43"));

        assertThat(krw).isEqualByComparingTo("296086");
    }

    @Test
    @DisplayName("USD_133을_sellRate_1474_47으로_KRW로_환산하면_196104이다")
    void USD_133을_sellRate_1474_47으로_KRW로_환산하면_196104이다() {
        BigDecimal krw = MoneyCalculator.toKrw(new BigDecimal("133"), new BigDecimal("1474.47"));

        assertThat(krw).isEqualByComparingTo("196104");
    }

    @Test
    @DisplayName("KRW_절사는_절대_올림되지_않는다")
    void KRW_절사는_절대_올림되지_않는다() {
        BigDecimal krw = MoneyCalculator.toKrw(new BigDecimal("1"), new BigDecimal("1.999"));

        assertThat(krw).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("JPY_1엔_기준_1000엔을_100엔_환율_910_50으로_환산하면_9105_KRW다")
    void JPY_1엔_기준_1000엔을_100엔_환율_910_50으로_환산하면_9105_KRW다() {
        BigDecimal krw = MoneyCalculator.toKrwFromJpy(
                new BigDecimal("1000"), new BigDecimal("910.50"));

        // 1000 × 910.50 / 100 = 9105.00 → floor → 9105
        assertThat(krw).isEqualByComparingTo("9105");
    }

    @Test
    @DisplayName("JPY_133엔을_100엔_환율_910_50으로_환산하면_floor_적용되어_1210_KRW다")
    void JPY_133엔을_100엔_환율_910_50으로_환산하면_floor_적용되어_1210_KRW다() {
        // 133 × 910.50 / 100 = 1210.965 → floor → 1210
        BigDecimal krw = MoneyCalculator.toKrwFromJpy(
                new BigDecimal("133"), new BigDecimal("910.50"));

        assertThat(krw).isEqualByComparingTo("1210");
    }

    @Test
    @DisplayName("KRW_금액을_환율로_나누어_외화로_환산할_수_있다")
    void KRW_금액을_환율로_나누어_외화로_환산할_수_있다() {
        BigDecimal forex = MoneyCalculator.toForex(new BigDecimal("296086"), new BigDecimal("1480.43"));

        assertThat(forex).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("환율이_0이면_외화_환산은_INVALID_RATE_BusinessException이다")
    void 환율이_0이면_외화_환산은_INVALID_RATE_BusinessException이다() {
        assertThatThrownBy(() ->
                MoneyCalculator.toForex(new BigDecimal("100"), BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_RATE);
    }

    @Test
    @DisplayName("null_입력은_INVALID_DATA_BusinessException이다")
    void null_입력은_INVALID_DATA_BusinessException이다() {
        assertThatThrownBy(() -> MoneyCalculator.roundRate(null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_DATA);
        assertThatThrownBy(() -> MoneyCalculator.buyRate(null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> MoneyCalculator.sellRate(null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> MoneyCalculator.toKrw(null, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> MoneyCalculator.toForex(BigDecimal.ONE, null))
                .isInstanceOf(BusinessException.class);
    }
}
