package com.switchwon.api.repository;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
class ExchangeRateHistoryRepositoryTest {

    @Autowired
    private ExchangeRateHistoryRepository repository;

    @Test
    @DisplayName("통화별_가장_최신_환율_1건이_currency_buy_sell_collectedAt까지_정확히_반환된다")
    void 통화별_가장_최신_환율_1건이_currency_buy_sell_collectedAt까지_정확히_반환된다() {
        LocalDateTime base = LocalDateTime.of(2026, 4, 28, 11, 0);
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("1470.00"), base));
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("1480.00"), base.plusMinutes(1)));
        LocalDateTime latestAt = base.plusMinutes(2);
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("1475.00"), latestAt));

        Optional<ExchangeRateHistory> latest =
                repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.USD);

        assertThat(latest).isPresent();
        ExchangeRateHistory found = latest.get();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCurrency()).isEqualTo(Currency.USD);
        assertThat(found.getTradeStanRate()).isEqualByComparingTo("1475.00");
        assertThat(found.getBuyRate()).isEqualByComparingTo("1548.75");
        assertThat(found.getSellRate()).isEqualByComparingTo("1401.25");
        assertThat(found.getCollectedAt()).isEqualTo(latestAt);
    }

    @Test
    @DisplayName("다른_통화의_환율은_조회_결과에_섞이지_않는다")
    void 다른_통화의_환율은_조회_결과에_섞이지_않는다() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("1480.00"), now));
        repository.save(ExchangeRateHistory.of(Currency.JPY, new BigDecimal("910.00"), now.plusMinutes(10)));

        Optional<ExchangeRateHistory> usd =
                repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.USD);

        assertThat(usd).isPresent();
        assertThat(usd.get().getCurrency()).isEqualTo(Currency.USD);
        assertThat(usd.get().getTradeStanRate()).isEqualByComparingTo("1480.00");
    }

    @Test
    @DisplayName("당일_자정_이후_데이터가_있으면_existsByCurrencyAndCollectedAtGreaterThanEqual은_true다")
    void 당일_자정_이후_데이터가_있으면_existsBy는_true다() {
        LocalDateTime today1100 = LocalDate.now().atTime(11, 0);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("1480.00"), today1100));

        boolean exists = repository.existsByCurrencyAndCollectedAtGreaterThanEqual(
                Currency.USD, todayStart);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("당일_데이터가_없으면_existsByCurrencyAndCollectedAtGreaterThanEqual은_false다")
    void 당일_데이터가_없으면_existsBy는_false다() {
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(15, 0);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        repository.save(ExchangeRateHistory.of(Currency.USD, new BigDecimal("1480.00"), yesterday));

        boolean exists = repository.existsByCurrencyAndCollectedAtGreaterThanEqual(
                Currency.USD, todayStart);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("환율이_없는_통화_조회는_Optional_empty를_반환한다")
    void 환율이_없는_통화_조회는_Optional_empty를_반환한다() {
        Optional<ExchangeRateHistory> result =
                repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.EUR);

        assertThat(result).isEmpty();
    }
}
