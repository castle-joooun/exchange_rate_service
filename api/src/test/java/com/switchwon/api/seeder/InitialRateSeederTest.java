package com.switchwon.api.seeder;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.service.ExchangeRateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(InitialRateSeederTest.TestConfig.class)
class InitialRateSeederTest {

    @Autowired
    private InitialRateSeeder seeder;
    @Autowired
    private ExchangeRateHistoryRepository repository;
    @MockBean
    private ExchangeRateClient exchangeRateClient;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("외부_API_정상_응답이면_4통화_모두_외부값으로_시드된다")
    void 외부_API_정상_응답이면_4통화_모두_외부값으로_시드된다() {
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1066.90")),
                new DailyExchangeRate("JPY(100)", new BigDecimal("951.05")),
                new DailyExchangeRate("CNH", new BigDecimal("163.65")),
                new DailyExchangeRate("EUR", new BigDecimal("1286.91"))
        ));

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(4);
        assertCurrencyRate(Currency.USD, "1066.90");
        assertCurrencyRate(Currency.JPY, "951.05");
        assertCurrencyRate(Currency.CNY, "163.65");
        assertCurrencyRate(Currency.EUR, "1286.91");
    }

    @Test
    @DisplayName("외부_API의_일부_통화가_falsy면_해당_통화만_하드코딩으로_보충된다")
    void 외부_API의_일부_통화가_falsy면_해당_통화만_하드코딩으로_보충된다() {
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("USD", new BigDecimal("1066.90")),
                new DailyExchangeRate("JPY(100)", BigDecimal.ZERO),    // falsy
                new DailyExchangeRate("CNH", null),                     // falsy
                new DailyExchangeRate("EUR", new BigDecimal("1286.91"))
        ));

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(4);
        assertCurrencyRate(Currency.USD, "1066.90");        // 외부값
        assertCurrencyRate(Currency.JPY, "925.43");         // 하드코딩 fallback
        assertCurrencyRate(Currency.CNY, "216.10");         // 하드코딩 fallback
        assertCurrencyRate(Currency.EUR, "1286.91");        // 외부값
    }

    @Test
    @DisplayName("외부_API_응답에_우리가_다루는_통화가_없으면_4통화_모두_하드코딩이다")
    void 외부_API_응답에_우리가_다루는_통화가_없으면_4통화_모두_하드코딩이다() {
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of(
                new DailyExchangeRate("GBP", new BigDecimal("1447.30")),
                new DailyExchangeRate("AUD", new BigDecimal("836.28"))
        ));

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(4);
        assertCurrencyRate(Currency.USD, "1477.78");
        assertCurrencyRate(Currency.JPY, "925.43");
        assertCurrencyRate(Currency.CNY, "216.10");
        assertCurrencyRate(Currency.EUR, "1728.21");
    }

    @Test
    @DisplayName("외부_API_빈_응답이면_4통화_모두_하드코딩이다")
    void 외부_API_빈_응답이면_4통화_모두_하드코딩이다() {
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class))).thenReturn(List.of());

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(4);
        assertCurrencyRate(Currency.USD, "1477.78");
        assertCurrencyRate(Currency.JPY, "925.43");
        assertCurrencyRate(Currency.CNY, "216.10");
        assertCurrencyRate(Currency.EUR, "1728.21");
    }

    @Test
    @DisplayName("외부_API_호출_자체가_예외를_던지면_4통화_모두_하드코딩으로_시드된다")
    void 외부_API_호출_자체가_예외를_던지면_4통화_모두_하드코딩으로_시드된다() {
        when(exchangeRateClient.fetchDailyRates(any(LocalDate.class)))
                .thenThrow(new RuntimeException("network down"));

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(4);
        assertCurrencyRate(Currency.USD, "1477.78");
        assertCurrencyRate(Currency.JPY, "925.43");
        assertCurrencyRate(Currency.CNY, "216.10");
        assertCurrencyRate(Currency.EUR, "1728.21");
    }

    @Test
    @DisplayName("DB에_이미_데이터가_있으면_외부_API_호출_없이_시드를_생략한다")
    void DB에_이미_데이터가_있으면_외부_API_호출_없이_시드를_생략한다() {
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("9999.99"), LocalDateTime.now()));

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(1);
        assertCurrencyRate(Currency.USD, "9999.99");
        org.mockito.Mockito.verify(exchangeRateClient, org.mockito.Mockito.never())
                .fetchDailyRates(any(LocalDate.class));
    }

    private void assertCurrencyRate(Currency currency, String expected) {
        ExchangeRateHistory row = repository.findTopByCurrencyOrderByCollectedAtDesc(currency).orElseThrow();
        assertThat(row.getTradeStanRate()).isEqualByComparingTo(expected);
    }

    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemDefaultZone();
        }

        @Bean
        InitialRateSeeder seeder(ExchangeRateHistoryRepository repository,
                                 ExchangeRateClient exchangeRateClient,
                                 Clock clock) {
            return new InitialRateSeeder(repository, exchangeRateClient, clock);
        }
    }
}
