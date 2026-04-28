package com.switchwon.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 환율/원화 환산의 모든 정밀도 규칙을 한 곳에서 관리한다.
 *
 * <ul>
 *   <li>환율 반올림: HALF_UP, scale 2 (소수점 둘째 자리)</li>
 *   <li>KRW 절사: FLOOR, scale 0 (소수점 이하 버림)</li>
 *   <li>전신환 매입율(buyRate) = 매매기준율 × 1.05 → 둘째 자리 반올림</li>
 *   <li>전신환 매도율(sellRate) = 매매기준율 × 0.95 → 둘째 자리 반올림</li>
 *   <li>JPY 100엔 환산 = 1엔 단위 시세 × 100 → 둘째 자리 반올림</li>
 * </ul>
 *
 * 외부에서는 절대 BigDecimal 산술을 직접 하지 말고 이 클래스만 경유한다.
 */
public final class MoneyCalculator {

    public static final int RATE_SCALE = 2;
    public static final int KRW_SCALE = 0;
    public static final BigDecimal BUY_SPREAD = new BigDecimal("1.05");
    public static final BigDecimal SELL_SPREAD = new BigDecimal("0.95");
    public static final BigDecimal JPY_UNIT = new BigDecimal("100");

    private MoneyCalculator() {}

    /**
     * 환율값을 소수점 둘째 자리까지 반올림한다.
     */
    public static BigDecimal roundRate(BigDecimal rate) {
        requireNonNull(rate, "rate");
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 매매기준율로부터 전신환 매입율(고객이 외화를 사는 환율)을 계산한다.
     */
    public static BigDecimal buyRate(BigDecimal tradeStanRate) {
        requireNonNull(tradeStanRate, "tradeStanRate");
        return roundRate(tradeStanRate.multiply(BUY_SPREAD));
    }

    /**
     * 매매기준율로부터 전신환 매도율(고객이 외화를 파는 환율)을 계산한다.
     */
    public static BigDecimal sellRate(BigDecimal tradeStanRate) {
        requireNonNull(tradeStanRate, "tradeStanRate");
        return roundRate(tradeStanRate.multiply(SELL_SPREAD));
    }

    /**
     * 1엔 단위 시세를 100엔 단위 환율로 환산한다.
     * 외부 API가 1엔 단위로만 시세를 줄 때 사용한다.
     * 한국수출입은행은 이미 JPY(100) 단위로 응답하므로 이 경우 호출 불필요.
     */
    public static BigDecimal toJpyHundredUnit(BigDecimal perYenRate) {
        requireNonNull(perYenRate, "perYenRate");
        return roundRate(perYenRate.multiply(JPY_UNIT));
    }

    /**
     * 외화 금액을 KRW로 환산한다. 소수점 이하는 버림(Floor).
     *
     * @param forexAmount 외화 금액 (예: USD 200)
     * @param appliedRate 적용 환율 (buyRate or sellRate)
     * @return KRW 환산 금액 (정수)
     */
    public static BigDecimal toKrw(BigDecimal forexAmount, BigDecimal appliedRate) {
        requireNonNull(forexAmount, "forexAmount");
        requireNonNull(appliedRate, "appliedRate");
        return forexAmount.multiply(appliedRate).setScale(KRW_SCALE, RoundingMode.FLOOR);
    }

    /**
     * KRW 금액을 외화로 환산한다. 외화는 소수점 둘째 자리까지 반올림한다.
     *
     * @param krwAmount   원화 금액
     * @param appliedRate 적용 환율
     * @return 외화 금액 (소수점 둘째 자리)
     */
    public static BigDecimal toForex(BigDecimal krwAmount, BigDecimal appliedRate) {
        requireNonNull(krwAmount, "krwAmount");
        requireNonNull(appliedRate, "appliedRate");
        if (appliedRate.signum() == 0) {
            throw new IllegalArgumentException("appliedRate must not be zero");
        }
        return krwAmount.divide(appliedRate, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static void requireNonNull(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
