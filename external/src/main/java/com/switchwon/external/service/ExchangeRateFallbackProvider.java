package com.switchwon.external.service;

import com.switchwon.external.dto.DailyExchangeRate;

import java.util.List;

/**
 * 외부 API 호출이 실패(서킷 OPEN, 4xx/5xx, 타임아웃)했을 때 환율 데이터를 대체 제공한다.
 *
 * 구현은 api 모듈에서 한다 (LatestRateFallbackProvider).
 * external 은 DB 도 random 로직도 알지 못한다 — 의존성 방향 유지를 위한 인터페이스.
 *
 * 구현체는 보통 "DB에 저장된 가장 최근 row 기반 ±0.3% 랜덤 변동값"을 반환한다.
 */
public interface ExchangeRateFallbackProvider {

    /**
     * 시스템에서 다루는 4개 통화(USD/JPY/CNY/EUR) 모두에 대해 대체 환율을 반환한다.
     * DB에 row가 전혀 없으면 빈 리스트 반환 가능.
     */
    List<DailyExchangeRate> getLatestAsRandomized();
}
