package com.switchwon.api.repository;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    /**
     * 통화의 가장 최신 환율 1건.
     * 환율 조회 API와 Mock 변동 생성 시 base row를 가져올 때 사용된다.
     */
    Optional<ExchangeRateHistory> findTopByCurrencyOrderByCollectedAtDesc(Currency currency);

    /**
     * 특정 통화에 대해, 주어진 시점 이후로 수집된 데이터가 1건이라도 있는지 여부.
     * 수집 정책에서 "오늘 자정 이후 데이터가 있는가?" 분기에 사용된다.
     */
    boolean existsByCurrencyAndCollectedAtGreaterThanEqual(Currency currency, LocalDateTime threshold);
}
