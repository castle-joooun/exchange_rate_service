package com.switchwon.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Clock Bean. 정책/스케줄러/시드가 LocalDateTime.now() 대신 clock.instant()를 사용하여
 * 테스트에서 Clock.fixed() 로 대체할 수 있게 한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
