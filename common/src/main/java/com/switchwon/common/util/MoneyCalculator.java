package com.switchwon.common.util;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.exception.CommonErrorCode;
import com.switchwon.common.log.LoggingPatterns;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public final class MoneyCalculator {

    public static final int RATE_SCALE = 2;
    public static final int KRW_SCALE = 0;
    public static final BigDecimal BUY_SPREAD = new BigDecimal("1.05");
    public static final BigDecimal SELL_SPREAD = new BigDecimal("0.95");
    public static final BigDecimal JPY_UNIT = new BigDecimal("100");

    private MoneyCalculator() {}

    public static BigDecimal roundRate(BigDecimal rate) {
        requireNonNull(rate, "rate");
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal buyRate(BigDecimal tradeStanRate) {
        requireNonNull(tradeStanRate, "tradeStanRate");
        return roundRate(tradeStanRate.multiply(BUY_SPREAD));
    }

    public static BigDecimal sellRate(BigDecimal tradeStanRate) {
        requireNonNull(tradeStanRate, "tradeStanRate");
        return roundRate(tradeStanRate.multiply(SELL_SPREAD));
    }

    public static BigDecimal toJpyHundredUnit(BigDecimal perYenRate) {
        requireNonNull(perYenRate, "perYenRate");
        return roundRate(perYenRate.multiply(JPY_UNIT));
    }

    public static BigDecimal toKrw(BigDecimal forexAmount, BigDecimal appliedRate) {
        requireNonNull(forexAmount, "forexAmount");
        requireNonNull(appliedRate, "appliedRate");
        return forexAmount.multiply(appliedRate).setScale(KRW_SCALE, RoundingMode.FLOOR);
    }

    /**
     * JPY 1엔 단위로 입력된 금액을 100엔 단위 환율로 KRW 환산한다.
     * 예: 1000엔 × (rate per 100JPY) = 1000/100 × rate
     *
     * 주의: 호출자는 사전에 forexAmount &gt;= 100 임을 보장해야 한다.
     */
    public static BigDecimal toKrwFromJpy(BigDecimal jpyAmount, BigDecimal rateOf100Yen) {
        requireNonNull(jpyAmount, "jpyAmount");
        requireNonNull(rateOf100Yen, "rateOf100Yen");
        return jpyAmount.multiply(rateOf100Yen)
                .divide(JPY_UNIT, KRW_SCALE, RoundingMode.FLOOR);
    }

    public static BigDecimal toForex(BigDecimal krwAmount, BigDecimal appliedRate) {
        requireNonNull(krwAmount, "krwAmount");
        requireNonNull(appliedRate, "appliedRate");
        if (appliedRate.signum() == 0) {
            log.warn("{} 외화 환산 실패 사유=환율_0 krwAmount={}", LoggingPatterns.BIZ_FAIL, krwAmount);
            throw new BusinessException(CommonErrorCode.INVALID_RATE, "appliedRate must not be zero");
        }
        return krwAmount.divide(appliedRate, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static void requireNonNull(BigDecimal value, String name) {
        if (value == null) {
            log.warn("{} BigDecimal null 입력 name={}", LoggingPatterns.BIZ_FAIL, name);
            throw new BusinessException(CommonErrorCode.INVALID_DATA, name + " must not be null");
        }
    }
}
