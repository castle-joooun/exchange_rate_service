package com.switchwon.external.config;

import com.switchwon.external.handler.KoreaeximResponseErrorHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 한국수출입은행 환율 API 호출용 RestClient Bean.
 * baseUrl, timeout, 공통 헤더, ResponseErrorHandler 가 한 곳에서 조립된다.
 */
@Configuration
@EnableConfigurationProperties(KoreaeximProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient koreaeximRestClient(KoreaeximProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "switch-won/1.0")
                .defaultStatusHandler(new KoreaeximResponseErrorHandler())
                .build();
    }
}
