package com.switchwon.api.service;

import com.switchwon.common.log.LoggingPatterns;
import com.switchwon.external.config.KoreaeximProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 환율 수집 정책. 매분 깨어나는 스케줄러가 "지금 외부 API를 호출할지 / Mock 변동만 만들지" 결정에 위임한다.
 *
 * <p>분기 규칙:</p>
 * <ul>
 *   <li>현재 분이 0분 + 평일 + 시각이 callHours(예: 12,15,18,21)에 포함 → CALL_API</li>
 *   <li>그 외 모든 경우 → MOCK_FROM_LATEST</li>
 * </ul>
 *
 * <p>Clock 을 주입받아 시간 의존 분기를 테스트할 수 있게 했다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateCollectionPolicy {

    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final Clock clock;
    private final KoreaeximProperties koreaeximProperties;

    public CollectionDecision decide() {
        LocalDateTime now = LocalDateTime.now(clock);
        return decideAt(now);
    }

    /**
     * 테스트 용이성을 위해 패키지 가시성으로 시각을 직접 받는 분기 메서드를 노출한다.
     */
    CollectionDecision decideAt(LocalDateTime now) {
        Set<Integer> callHours = resolveCallHours();

        if (WEEKEND.contains(now.getDayOfWeek())) {
            log.debug("{} 수집 정책: 주말 → MOCK dayOfWeek={}",
                    LoggingPatterns.BIZ_EVENT, now.getDayOfWeek());
            return CollectionDecision.MOCK_FROM_LATEST;
        }

        if (now.getMinute() == 0 && callHours.contains(now.getHour())) {
            log.info("{} 수집 정책: 평일 호출 슬롯 적중 → CALL_API hour={}",
                    LoggingPatterns.BIZ_EVENT, now.getHour());
            return CollectionDecision.CALL_API;
        }

        log.debug("{} 수집 정책: 일반 분기 → MOCK now={}",
                LoggingPatterns.BIZ_EVENT, now);
        return CollectionDecision.MOCK_FROM_LATEST;
    }

    private Set<Integer> resolveCallHours() {
        List<Integer> configured = koreaeximProperties.callHours();
        if (configured == null || configured.isEmpty()) {
            log.warn("{} callHours 설정이 비어있어 외부 API 호출이 영원히 안 됩니다. yml 확인 필요",
                    LoggingPatterns.BIZ_FAIL);
            return Set.of();
        }
        return configured.stream().collect(Collectors.toUnmodifiableSet());
    }
}
