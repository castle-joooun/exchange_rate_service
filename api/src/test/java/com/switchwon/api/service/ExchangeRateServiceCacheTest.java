package com.switchwon.api.service;

import com.switchwon.api.config.CacheConfig;
import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.dto.ExchangeRateListResponse;
import com.switchwon.api.dto.ExchangeRateResponse;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @Cacheable 이 실제로 작동하는지 검증.
 * 같은 키로 두 번 호출 시 두 번째는 repository 가 호출되지 않아야 한다.
 */
@SpringBootTest(classes = ExchangeRateServiceCacheTest.MinimalApp.class)
@Import(CacheConfig.class)
class ExchangeRateServiceCacheTest {

    @Autowired
    private ExchangeRateService service;
    @Autowired
    private CacheManager cacheManager;
    @MockBean
    private ExchangeRateHistoryRepository repository;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("getLatest_같은_통화로_연속_호출하면_repository는_1회만_호출된다")
    void getLatest_같은_통화로_연속_호출하면_repository는_1회만_호출된다() {
        ExchangeRateHistory row = ExchangeRateHistory.of(
                Currency.USD, new BigDecimal("1477.45"), LocalDateTime.now());
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.USD))
                .thenReturn(Optional.of(row));

        ExchangeRateResponse first = service.getLatest(Currency.USD);
        ExchangeRateResponse second = service.getLatest(Currency.USD);
        ExchangeRateResponse third = service.getLatest(Currency.USD);

        assertThat(first.tradeStanRate()).isEqualByComparingTo("1477.45");
        assertThat(second).isSameAs(first); // 캐시 hit 시 동일 객체
        assertThat(third).isSameAs(first);
        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.USD);
    }

    @Test
    @DisplayName("getLatest_다른_통화는_별도_캐시_엔트리를_가진다")
    void getLatest_다른_통화는_별도_캐시_엔트리를_가진다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.USD))
                .thenReturn(Optional.of(ExchangeRateHistory.of(
                        Currency.USD, new BigDecimal("1477.45"), LocalDateTime.now())));
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.JPY))
                .thenReturn(Optional.of(ExchangeRateHistory.of(
                        Currency.JPY, new BigDecimal("910.50"), LocalDateTime.now())));

        service.getLatest(Currency.USD);
        service.getLatest(Currency.JPY);
        service.getLatest(Currency.USD);
        service.getLatest(Currency.JPY);

        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.USD);
        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.JPY);
    }

    @Test
    @DisplayName("getLatestAll_연속_호출시_repository는_각_통화당_1회만_호출된다")
    void getLatestAll_연속_호출시_repository는_각_통화당_1회만_호출된다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> Optional.of(ExchangeRateHistory.of(
                        inv.getArgument(0), new BigDecimal("1000.00"), LocalDateTime.now())));

        ExchangeRateListResponse first = service.getLatestAll();
        ExchangeRateListResponse second = service.getLatestAll();

        assertThat(second).isSameAs(first);
        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.USD);
        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.JPY);
        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.CNY);
        verify(repository, times(1)).findTopByCurrencyOrderByCollectedAtDesc(Currency.EUR);
    }

    @Test
    @DisplayName("getLatest_캐시_clear_후_repository가_다시_호출된다")
    void getLatest_캐시_clear_후_repository가_다시_호출된다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.USD))
                .thenReturn(Optional.of(ExchangeRateHistory.of(
                        Currency.USD, new BigDecimal("1477.45"), LocalDateTime.now())));

        service.getLatest(Currency.USD);
        cacheManager.getCache(CacheConfig.LATEST_EXCHANGE_RATES).clear();
        service.getLatest(Currency.USD);

        verify(repository, times(2)).findTopByCurrencyOrderByCollectedAtDesc(Currency.USD);
    }

    @TestConfiguration
    @EnableCaching
    static class MinimalApp {

        @Bean
        ExchangeRateService exchangeRateService(ExchangeRateHistoryRepository repository) {
            return new ExchangeRateService(repository);
        }
    }
}
