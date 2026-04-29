package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.service.ExchangeRateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExchangeRateCollectorTest {

    private RateCollectionPolicy policy;
    private ExchangeRateClient exchangeRateClient;
    private LatestRateFallbackProvider fallbackProvider;
    private ExchangeRateHistoryRepository repository;
    private ExchangeRateCollector collector;

    @BeforeEach
    void setUp() {
        policy = mock(RateCollectionPolicy.class);
        exchangeRateClient = mock(ExchangeRateClient.class);
        fallbackProvider = mock(LatestRateFallbackProvider.class);
        repository = mock(ExchangeRateHistoryRepository.class);
        collector = new ExchangeRateCollector(
                policy, exchangeRateClient, fallbackProvider, repository, Clock.systemDefaultZone());
    }

    @Test
    @DisplayName("CALL_API_결정시_external_Client만_호출되고_FallbackProvider는_호출되지_않는다")
    void CALL_API_결정시_external_Client만_호출되고_FallbackProvider는_호출되지_않는다() {
        when(policy.decide()).thenReturn(CollectionDecision.CALL_API);
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1480.00"))));

        collector.collectOnce();

        verify(exchangeRateClient, times(1)).fetchDailyRates(any(LocalDate.class));
        verify(fallbackProvider, never()).getLatestAsRandomized();
    }

    @Test
    @DisplayName("MOCK_FROM_LATEST_결정시_FallbackProvider만_호출되고_external_Client는_호출되지_않는다")
    void MOCK_FROM_LATEST_결정시_FallbackProvider만_호출되고_external_Client는_호출되지_않는다() {
        when(policy.decide()).thenReturn(CollectionDecision.MOCK_FROM_LATEST);
        when(fallbackProvider.getLatestAsRandomized()).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1480.00"))));

        collector.collectOnce();

        verify(fallbackProvider, times(1)).getLatestAsRandomized();
        verify(exchangeRateClient, never()).fetchDailyRates(any(LocalDate.class));
    }

    @Test
    @DisplayName("수집_결과가_있으면_통화별로_repository_save가_호출된다")
    void 수집_결과가_있으면_통화별로_repository_save가_호출된다() {
        when(policy.decide()).thenReturn(CollectionDecision.CALL_API);
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1480.00")),
                new DailyExchangeRate("JPY(100)", new BigDecimal("910.00")),
                new DailyExchangeRate("CNH", new BigDecimal("202.50")),
                new DailyExchangeRate("EUR", new BigDecimal("1600.00"))
        ));

        collector.collectOnce();

        verify(repository, times(4)).save(any(ExchangeRateHistory.class));
    }

    @Test
    @DisplayName("수집_결과가_빈_리스트면_repository_save는_호출되지_않는다")
    void 수집_결과가_빈_리스트면_repository_save는_호출되지_않는다() {
        when(policy.decide()).thenReturn(CollectionDecision.MOCK_FROM_LATEST);
        when(fallbackProvider.getLatestAsRandomized()).thenReturn(List.of());

        collector.collectOnce();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("JPY_100은_JPY로_매핑되어_저장된다")
    void JPY_100은_JPY로_매핑되어_저장된다() {
        when(policy.decide()).thenReturn(CollectionDecision.CALL_API);
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("JPY(100)", new BigDecimal("910.50"))
        ));

        collector.collectOnce();

        ArgumentCaptor<ExchangeRateHistory> captor = ArgumentCaptor.forClass(ExchangeRateHistory.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.JPY);
        assertThat(captor.getValue().getTradeStanRate()).isEqualByComparingTo("910.50");
    }

    @Test
    @DisplayName("CNH는_CNY로_매핑되어_저장된다")
    void CNH는_CNY로_매핑되어_저장된다() {
        when(policy.decide()).thenReturn(CollectionDecision.CALL_API);
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("CNH", new BigDecimal("202.50"))
        ));

        collector.collectOnce();

        ArgumentCaptor<ExchangeRateHistory> captor = ArgumentCaptor.forClass(ExchangeRateHistory.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(Currency.CNY);
    }

    @Test
    @DisplayName("미지원_통화_GBP가_섞여있으면_해당_통화만_스킵하고_나머지는_저장된다")
    void 미지원_통화_GBP가_섞여있으면_해당_통화만_스킵하고_나머지는_저장된다() {
        when(policy.decide()).thenReturn(CollectionDecision.CALL_API);
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1480.00")),
                new DailyExchangeRate("GBP", new BigDecimal("1815.00")),
                new DailyExchangeRate("EUR", new BigDecimal("1600.00"))
        ));

        collector.collectOnce();

        verify(repository, times(2)).save(any(ExchangeRateHistory.class));
    }

    @Test
    @DisplayName("저장된_엔티티의_buyRate_sellRate가_자동_계산되어_저장된다")
    void 저장된_엔티티의_buyRate_sellRate가_자동_계산되어_저장된다() {
        when(policy.decide()).thenReturn(CollectionDecision.CALL_API);
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1477.45"))
        ));

        collector.collectOnce();

        ArgumentCaptor<ExchangeRateHistory> captor = ArgumentCaptor.forClass(ExchangeRateHistory.class);
        verify(repository, times(1)).save(captor.capture());
        ExchangeRateHistory saved = captor.getValue();
        assertThat(saved.getTradeStanRate()).isEqualByComparingTo("1477.45");
        assertThat(saved.getBuyRate()).isEqualByComparingTo("1551.32");
        assertThat(saved.getSellRate()).isEqualByComparingTo("1403.58");
    }
}
