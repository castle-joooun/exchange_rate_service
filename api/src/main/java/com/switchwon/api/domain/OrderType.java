package com.switchwon.api.domain;

import com.switchwon.api.exception.DomainErrorCode;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import lombok.extern.slf4j.Slf4j;

/**
 * 외화 주문 종류.
 *
 * <ul>
 *   <li>BUY  — 고객이 외화를 사는 경우 (KRW → 외화), 적용 환율은 buyRate</li>
 *   <li>SELL — 고객이 외화를 파는 경우 (외화 → KRW), 적용 환율은 sellRate</li>
 * </ul>
 */
@Slf4j
public enum OrderType {

    BUY,
    SELL;

    public static OrderType resolve(Currency from, Currency to) {
        if (from == Currency.KRW && to != null && to.isForex()) {
            log.info("{} 주문 타입 결정 from={} to={} type=BUY",
                    LoggingPatterns.BIZ_EVENT, from, to);
            return BUY;
        }
        if (from != null && from.isForex() && to == Currency.KRW) {
            log.info("{} 주문 타입 결정 from={} to={} type=SELL",
                    LoggingPatterns.BIZ_EVENT, from, to);
            return SELL;
        }

        log.warn("{} 잘못된 통화 조합으로 주문 타입 결정 실패 from={} to={}",
                LoggingPatterns.BIZ_FAIL, from, to);
        throw new BusinessException(
                DomainErrorCode.INVALID_CURRENCY_PAIR,
                String.format("주문은 KRW와 외화 사이에서만 가능합니다. from=%s, to=%s", from, to));
    }
}
