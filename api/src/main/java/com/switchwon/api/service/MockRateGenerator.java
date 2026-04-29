package com.switchwon.api.service;

import com.switchwon.common.util.MoneyCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 기준 매매기준율로부터 ±0.3% 범위의 랜덤 변동값을 생성한다.
 * 정밀도(둘째 자리 반올림)는 MoneyCalculator 가 보장한다.
 */
@Component
public class MockRateGenerator {

    /** 변동 폭. ±0.3% = 0.003 */
    public static final double VARIANCE_RATIO = 0.003;

    /**
     * @param baseTradeStanRate 직전 매매기준율 (둘째 자리 반올림 상태)
     * @return 새 매매기준율 (둘째 자리 반올림)
     */
    public BigDecimal generate(BigDecimal baseTradeStanRate) {
        return generate(baseTradeStanRate, ThreadLocalRandom.current().nextDouble(-VARIANCE_RATIO, VARIANCE_RATIO));
    }

    /**
     * 테스트에서 변동 비율을 결정적으로 주입하기 위한 메서드.
     * @param ratio -0.003 ~ 0.003 범위 권장
     */
    BigDecimal generate(BigDecimal baseTradeStanRate, double ratio) {
        if (baseTradeStanRate == null) {
            throw new IllegalArgumentException("baseTradeStanRate must not be null");
        }
        BigDecimal multiplier = BigDecimal.valueOf(1.0 + ratio);
        BigDecimal next = baseTradeStanRate.multiply(multiplier);
        return MoneyCalculator.roundRate(next);
    }
}
