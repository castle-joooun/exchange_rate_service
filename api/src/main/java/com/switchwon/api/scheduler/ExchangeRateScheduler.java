package com.switchwon.api.scheduler;

import com.switchwon.api.service.ExchangeRateCollector;
import com.switchwon.common.log.LoggingPatterns;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 매분 0초에 발화하여 환율 수집 사이클을 트리거한다.
 *
 * 실제 분기(외부 API 호출 vs Mock)는 ExchangeRateCollector 에 위임한다.
 *
 * MDC traceId 를 발급하여 비동기 컨텍스트로 전달되지 않더라도, 적어도 동기 구간 로그는
 * 같은 사이클로 묶여 보이게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private static final String TRACE_ID_KEY = "traceId";

    private final ExchangeRateCollector collector;

    @Scheduled(cron = "0 * * * * *")
    public void collect() {
        String traceId = "scheduler-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID_KEY, traceId);
        try {
            log.info("{} 스케줄러 발화", LoggingPatterns.BIZ_EVENT);
            collector.collectOnce();
        } catch (Exception e) {
            log.error("{} 스케줄러 사이클 실패 reason={}",
                    LoggingPatterns.SYS_ERROR, e.getClass().getSimpleName(), e);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
