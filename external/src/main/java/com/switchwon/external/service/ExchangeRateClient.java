package com.switchwon.external.service;

import com.switchwon.external.dto.DailyExchangeRate;

import java.time.LocalDate;
import java.util.List;

/**
 * 외부 환율 API 추상화. api 모듈은 이 인터페이스만 의존한다.
 *
 * 구현체 교체(다른 환율 제공처로 변경) 시 api 모듈 코드는 영향받지 않는다.
 */
public interface ExchangeRateClient {

    /**
     * 특정 일자의 일일 환율을 조회한다.
     *
     * @param date 조회 일자
     * @return 매핑된 통화 환율 목록. 영업일 환율 미고시 시 빈 리스트 가능
     */
    List<DailyExchangeRate> fetchDailyRates(LocalDate date);
}
