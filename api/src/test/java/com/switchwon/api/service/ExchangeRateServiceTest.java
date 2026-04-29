package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.dto.ExchangeRateListResponse;
import com.switchwon.api.dto.ExchangeRateResponse;
import com.switchwon.api.exception.ExchangeRateErrorCode;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExchangeRateServiceTest {

    private ExchangeRateHistoryRepository repository;
    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExchangeRateHistoryRepository.class);
        service = new ExchangeRateService(repository);
    }

    @Test
    @DisplayName("getLatest는_repository_최신_row를_DTO로_변환하여_반환한다")
    void getLatest는_repository_최신_row를_DTO로_변환하여_반환한다() {
        LocalDateTime collectedAt = LocalDateTime.of(2026, 4, 28, 12, 0);
        ExchangeRateHistory history = ExchangeRateHistory.of(
                Currency.USD, new BigDecimal("1477.45"), collectedAt);
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.USD))
                .thenReturn(Optional.of(history));

        ExchangeRateResponse response = service.getLatest(Currency.USD);

        assertThat(response.currency()).isEqualTo(Currency.USD);
        assertThat(response.tradeStanRate()).isEqualByComparingTo("1477.45");
        assertThat(response.buyRate()).isEqualByComparingTo("1551.32");
        assertThat(response.sellRate()).isEqualByComparingTo("1403.58");
        assertThat(response.dateTime()).isEqualTo(collectedAt);
    }

    @Test
    @DisplayName("getLatest_통화_데이터_없으면_EXCHANGE_RATE_NOT_FOUND_BusinessException이다")
    void getLatest_통화_데이터_없으면_EXCHANGE_RATE_NOT_FOUND_BusinessException이다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.EUR))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(Currency.EUR))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND);
    }

    @Test
    @DisplayName("getLatest_KRW_조회는_데이터가_없으므로_EXCHANGE_RATE_NOT_FOUND다")
    void getLatest_KRW_조회는_데이터가_없으므로_EXCHANGE_RATE_NOT_FOUND다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.KRW))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(Currency.KRW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND);
    }

    @Test
    @DisplayName("getLatestAll은_4개_외화의_환율을_반환한다")
    void getLatestAll은_4개_외화의_환율을_반환한다() {
        stubLatestRow(Currency.USD, "1477.45");
        stubLatestRow(Currency.JPY, "910.50");
        stubLatestRow(Currency.CNY, "202.50");
        stubLatestRow(Currency.EUR, "1600.00");

        ExchangeRateListResponse result = service.getLatestAll();

        assertThat(result.exchangeRateList()).hasSize(4);
        assertThat(result.exchangeRateList()).extracting(ExchangeRateResponse::currency)
                .containsExactlyInAnyOrder(Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR);
    }

    @Test
    @DisplayName("getLatestAll에서_일부_통화_데이터가_없으면_있는_것만_반환한다")
    void getLatestAll에서_일부_통화_데이터가_없으면_있는_것만_반환한다() {
        stubLatestRow(Currency.USD, "1477.45");
        stubLatestRow(Currency.EUR, "1600.00");
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.JPY))
                .thenReturn(Optional.empty());
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(Currency.CNY))
                .thenReturn(Optional.empty());

        ExchangeRateListResponse result = service.getLatestAll();

        assertThat(result.exchangeRateList()).hasSize(2);
        assertThat(result.exchangeRateList()).extracting(ExchangeRateResponse::currency)
                .containsExactlyInAnyOrder(Currency.USD, Currency.EUR);
    }

    @Test
    @DisplayName("getLatestAll에서_모든_통화_데이터가_없으면_빈_리스트를_반환한다")
    void getLatestAll에서_모든_통화_데이터가_없으면_빈_리스트를_반환한다() {
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        ExchangeRateListResponse result = service.getLatestAll();

        assertThat(result.exchangeRateList()).isEmpty();
    }

    private void stubLatestRow(Currency currency, String rate) {
        ExchangeRateHistory history = ExchangeRateHistory.of(
                currency, new BigDecimal(rate), LocalDateTime.now());
        when(repository.findTopByCurrencyOrderByCollectedAtDesc(currency))
                .thenReturn(Optional.of(history));
    }
}
