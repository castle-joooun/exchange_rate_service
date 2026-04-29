package com.switchwon.api.service;

/**
 * 환율 수집 시 외부 API 를 부를지, Mock 으로 변동시킬지를 결정한다.
 */
public enum CollectionDecision {

    /** 외부 API 호출. 성공 시 응답 그대로 저장, 실패 시 서킷브레이커 fallback 으로 자동 전환. */
    CALL_API,

    /** DB의 가장 최근 row 기반 ±0.3% 랜덤 변동값 생성 후 저장. */
    MOCK_FROM_LATEST
}
