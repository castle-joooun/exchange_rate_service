package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.external.dto.DailyExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LatestRateFallbackProviderTest {

    private ExchangeRateHistoryRepository repository;
    private MockRateGenerator generator;
    private LatestRateFallbackProvider provider;

    @BeforeEach
    void setUp() {
        repository = mock(ExchangeRateHistoryRepository.class);
        generator = mock(MockRateGenerator.class);
        provider = new LatestRateFallbackProvider(repository, generator);
    }

    @Test
    @DisplayName("4개_통화_모두_DB에_있으면_4건의_DailyExchangeRate를_반환한다")
    void four_currencies_all_present_returns_four_items() {
        stubLatestRow(Currency.USD, "1480.00");
        stubLatestRow(Currency.JPY, "910.00");
        stubLatestRow(Currency.CNY, "200.00");
        stubLatestRow(Currency.EUR, "1600.00");
        when(generator.generate(any(BigDecimal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<DailyExchangeRate> result = provider.getLatestAsRandomized();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(DailyExchangeRate::currencyCode)
                .containsExactlyInAnyOrder("USD", "JPY(100)", "CNH", "EUR");
    }

    @Test
    @DisplayName("JPY는_currencyCode가_JPY_100으로_역매핑된다")
    void JPY는_currencyCode가_JPY_100으로_역매핑된다() {
        stubLatestRow(Currency.JPY, "910.00");
        when(generator.generate(any(BigDecimal.class))).thenReturn(new BigDecimal("910.50"));

        List<DailyExchangeRate> result = provider.getLatestAsRandomized();

        assertThat(result).singleElement()
                .satisfies(r -> {
                    assertThat(r.currencyCode()).isEqualTo("JPY(100)");
                    assertThat(r.tradeStanRate()).isEqualByComparingTo("910.50");
                });
    }

    @Test
    @DisplayName("CNY는_currencyCode가_CNH로_역매핑된다")
    void CNY는_currencyCode가_CNH로_역매핑된다() {
        stubLatestRow(Currency.CNY, "200.00");
        when(generator.generate(any(BigDecimal.class))).thenReturn(new BigDecimal("200.30"));

        List<DailyExchangeRate> result = provider.getLatestAsRandomized();

        assertThat(result).singleElement()
                .extracting(DailyExchangeRate::currencyCode)
                .isEqualTo("CNH");
    }

    @Test
    @DisplayName("DB가_완전히_비어있으면_빈_리스트를_반환한다")
    void DB가_완전히_비어있으면_빈_리스트를_반환한다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(any())).thenReturn(Optional.empty());

        List<DailyExchangeRate> result = provider.getLatestAsRandomized();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일부_통화만_DB에_있으면_있는_것만_반환한다")
    void 일부_통화만_DB에_있으면_있는_것만_반환한다() {
        stubLatestRow(Currency.USD, "1480.00");
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.JPY)).thenReturn(Optional.empty());
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.CNY)).thenReturn(Optional.empty());
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.EUR)).thenReturn(Optional.empty());
        when(generator.generate(any(BigDecimal.class))).thenAnswer(inv -> inv.getArgument(0));

        List<DailyExchangeRate> result = provider.getLatestAsRandomized();

        assertThat(result).singleElement()
                .extracting(DailyExchangeRate::currencyCode)
                .isEqualTo("USD");
    }

    private void stubLatestRow(Currency currency, String rate) {
        ExchangeRateHistory history = ExchangeRateHistory.of(currency, new BigDecimal(rate), LocalDateTime.now());
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(currency))
                .thenReturn(Optional.of(history));
    }
}
