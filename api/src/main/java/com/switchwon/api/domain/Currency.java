package com.switchwon.api.domain;

import com.switchwon.api.exception.DomainErrorCode;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.common.log.LoggingPatterns;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 시스템에서 다루는 통화 종류.
 * 과제 대상 4개 외화 + 기준 통화 KRW.
 */
@Slf4j
public enum Currency {

    KRW,
    USD,
    JPY,
    CNY,
    EUR;

    /**
     * 한국수출입은행 API 응답의 cur_unit 값을 시스템 Currency로 매핑한다.
     *
     * <ul>
     *   <li>JPY(100) → JPY (이미 100엔 단위로 응답)</li>
     *   <li>CNH → CNY (위안화 표기 통일)</li>
     *   <li>그 외는 동일 코드</li>
     * </ul>
     */
    public static Currency fromExternalCode(String externalCode) {
        if (externalCode == null) {
            log.warn("{} 외부 통화 코드 매핑 실패 사유=null", LoggingPatterns.BIZ_FAIL);
            throw new BusinessException(DomainErrorCode.UNSUPPORTED_CURRENCY, "externalCode is null");
        }
        String normalized = externalCode.trim().toUpperCase();
        if (normalized.startsWith("JPY")) {
            log.info("{} 통화 매핑 externalCode={} resolved=JPY", LoggingPatterns.BIZ_EVENT, externalCode);
            return JPY;
        }
        if (normalized.equals("CNH")) {
            log.info("{} 통화 매핑 externalCode={} resolved=CNY", LoggingPatterns.BIZ_EVENT, externalCode);
            return CNY;
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("{} 지원하지 않는 통화 externalCode={}", LoggingPatterns.BIZ_FAIL, externalCode);
                    return new BusinessException(
                            DomainErrorCode.UNSUPPORTED_CURRENCY,
                            "지원하지 않는 통화: " + externalCode);
                });
    }

    public boolean isForex() {
        return this != KRW;
    }
}
