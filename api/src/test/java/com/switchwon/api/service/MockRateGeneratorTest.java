package com.switchwon.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockRateGeneratorTest {

    private final MockRateGenerator generator = new MockRateGenerator();

    @Test
    @DisplayName("ratio가_0이면_base와_동일한_값이_반환된다")
    void ratio가_0이면_base와_동일한_값이_반환된다() {
        BigDecimal result = generator.generate(new BigDecimal("1477.45"), 0.0);

        assertThat(result).isEqualByComparingTo("1477.45");
    }

    @Test
    @DisplayName("ratio가_양수면_base보다_큰_값이_반환된다")
    void ratio가_양수면_base보다_큰_값이_반환된다() {
        BigDecimal result = generator.generate(new BigDecimal("1000.00"), 0.003);

        // 1000 * 1.003 = 1003.00
        assertThat(result).isEqualByComparingTo("1003.00");
    }

    @Test
    @DisplayName("ratio가_음수면_base보다_작은_값이_반환된다")
    void ratio가_음수면_base보다_작은_값이_반환된다() {
        BigDecimal result = generator.generate(new BigDecimal("1000.00"), -0.003);

        // 1000 * 0.997 = 997.00
        assertThat(result).isEqualByComparingTo("997.00");
    }

    @Test
    @DisplayName("결과는_둘째_자리에서_반올림된다")
    void 결과는_둘째_자리에서_반올림된다() {
        BigDecimal result = generator.generate(new BigDecimal("1477.45"), 0.001);

        // 1477.45 * 1.001 = 1478.92745 → 1478.93
        assertThat(result).isEqualByComparingTo("1478.93");
    }

    @RepeatedTest(20)
    @DisplayName("랜덤_생성_결과는_항상_base의_±0_3%_범위_내에_들어온다")
    void 랜덤_생성_결과는_항상_base의_변동_범위_내에_들어온다() {
        BigDecimal base = new BigDecimal("1000.00");

        BigDecimal result = generator.generate(base);

        BigDecimal minAllowed = base.multiply(new BigDecimal("0.997"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxAllowed = base.multiply(new BigDecimal("1.003"))
                .setScale(2, RoundingMode.HALF_UP);

        assertThat(result).isBetween(minAllowed, maxAllowed);
    }

    @Test
    @DisplayName("base가_null이면_IllegalArgumentException이다")
    void base가_null이면_IllegalArgumentException이다() {
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
