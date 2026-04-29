package com.switchwon.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 한국수출입은행 환율 API 연동 설정.
 *
 * application.yml 의 external.koreaexim.* 와 매핑된다.
 *
 * @param baseUrl          API 기본 URL
 * @param authKey          인증 키 (환경변수 override 가능)
 * @param connectTimeoutMs HTTP connect timeout
 * @param readTimeoutMs    HTTP read timeout
 * @param callHours        외부 API 를 호출할 시각 목록. 평일 매시 0분에 해당 시각이면 호출.
 *                         예: [12, 15, 18, 21]
 */
@ConfigurationProperties(prefix = "external.koreaexim")
public record KoreaeximProperties(
        String baseUrl,
        String authKey,
        int connectTimeoutMs,
        int readTimeoutMs,
        List<Integer> callHours
) {
}
