package com.switchwon.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 한국수출입은행 환율 API 연동 설정.
 *
 * application.yml 의 external.koreaexim.* 와 매핑된다.
 */
@ConfigurationProperties(prefix = "external.koreaexim")
public record KoreaeximProperties(
        String baseUrl,
        String authKey,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
