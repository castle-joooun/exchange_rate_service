package com.switchwon.api.scheduler;

import com.switchwon.api.service.ExchangeRateCollector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExchangeRateSchedulerTest {

    @Test
    @DisplayName("스케줄러가_발화하면_Collector_collectOnce가_정확히_1회_호출된다")
    void 스케줄러가_발화하면_Collector_collectOnce가_정확히_1회_호출된다() {
        ExchangeRateCollector collector = mock(ExchangeRateCollector.class);
        ExchangeRateScheduler scheduler = new ExchangeRateScheduler(collector);

        scheduler.collect();

        verify(collector, times(1)).collectOnce();
    }

    @Test
    @DisplayName("Collector가_예외를_던져도_스케줄러는_예외를_삼키고_정상_종료한다")
    void Collector가_예외를_던져도_스케줄러는_예외를_삼키고_정상_종료한다() {
        ExchangeRateCollector collector = mock(ExchangeRateCollector.class);
        doThrow(new RuntimeException("simulated")).when(collector).collectOnce();
        ExchangeRateScheduler scheduler = new ExchangeRateScheduler(collector);

        // 예외가 밖으로 던져지면 다음 분 발화에 영향. 테스트는 정상 종료여야 함.
        scheduler.collect();

        verify(collector, times(1)).collectOnce();
    }
}
