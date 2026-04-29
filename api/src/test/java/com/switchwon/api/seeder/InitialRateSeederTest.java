package com.switchwon.api.seeder;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(InitialRateSeederTest.TestConfig.class)
class InitialRateSeederTest {

    @Autowired
    private InitialRateSeeder seeder;
    @Autowired
    private ExchangeRateHistoryRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("DB가_비어_있으면_4개_기준_환율이_시드된다")
    void DB가_비어_있으면_4개_기준_환율이_시드된다() {
        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(4);
        assertCurrencyRate(Currency.USD, "1477.78");
        assertCurrencyRate(Currency.JPY, "925.43");
        assertCurrencyRate(Currency.CNY, "216.10");
        assertCurrencyRate(Currency.EUR, "1728.21");
    }

    @Test
    @DisplayName("DB에_이미_데이터가_있으면_시드를_생략한다")
    void DB에_이미_데이터가_있으면_시드를_생략한다() {
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("9999.99"), LocalDateTime.now()));

        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(1);
        // 시드의 1477.78이 아니라 기존 9999.99가 유지된다
        assertCurrencyRate(Currency.USD, "9999.99");
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
        InitialRateSeeder seeder(ExchangeRateHistoryRepository repository, Clock clock) {
            return new InitialRateSeeder(repository, clock);
        }
    }
}
