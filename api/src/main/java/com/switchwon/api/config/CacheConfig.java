package com.switchwon.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 환율 조회 API 가 사용하는 캐시 설정.
 *
 * <ul>
 *   <li>cache name: {@value #LATEST_EXCHANGE_RATES}</li>
 *   <li>TTL: 60초 — 스케줄러 주기와 일치. 다음 사이클이 들어오면 자동으로 stale 만료</li>
 *   <li>maximumSize: 8 — 통화 4종 × 단건/전체 키 정도</li>
 * </ul>
 *
 * <p>명시적 evict 는 사용하지 않는다. 60초 TTL 이 충분히 짧아 다음 사이클이 바로 보이게 된다.</p>
 */
@Configuration
public class CacheConfig {

    public static final String LATEST_EXCHANGE_RATES = "latestExchangeRates";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(LATEST_EXCHANGE_RATES);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(8)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats());
        return manager;
    }
}
