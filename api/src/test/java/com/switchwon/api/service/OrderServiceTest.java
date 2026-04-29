package com.switchwon.api.service;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.domain.Order;
import com.switchwon.api.domain.OrderType;
import com.switchwon.api.dto.OrderRequest;
import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.exception.ExchangeRateErrorCode;
import com.switchwon.api.exception.OrderErrorCode;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.api.repository.OrderRepository;
import com.switchwon.common.exception.BusinessException;
import com.switchwon.api.exception.DomainErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private ExchangeRateHistoryRepository exchangeRateRepository;
    private OrderRepository orderRepository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        exchangeRateRepository = mock(ExchangeRateHistoryRepository.class);
        orderRepository = mock(OrderRepository.class);
        service = new OrderService(exchangeRateRepository, orderRepository);

        // orderRepository.save() 가 입력을 그대로 반환하도록 stub
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("KRW에서_USD_매수_주문은_buyRate가_적용되고_KRW_금액이_floor_환산된다")
    void KRW에서_USD_매수_주문은_buyRate가_적용되고_KRW_금액이_floor_환산된다() {
        // 매매기준율 1477.45 → buyRate = 1551.32, sellRate = 1403.58
        stubLatestRate(Currency.USD, "1477.45");

        OrderCreatedResponse response = service.placeOrder(new OrderRequest(
                new BigDecimal("200"), Currency.KRW, Currency.USD));

        // 200 × 1551.32 = 310264.00 → floor → 310264
        assertThat(response.fromCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.toCurrency()).isEqualTo(Currency.USD);
        assertThat(response.fromAmount()).isEqualByComparingTo("310264");
        assertThat(response.toAmount()).isEqualByComparingTo("200");
        assertThat(response.tradeRate()).isEqualByComparingTo("1551.32");
    }

    @Test
    @DisplayName("USD에서_KRW_매도_주문은_sellRate가_적용되고_KRW_금액이_floor_환산된다")
    void USD에서_KRW_매도_주문은_sellRate가_적용되고_KRW_금액이_floor_환산된다() {
        stubLatestRate(Currency.USD, "1477.45");

        OrderCreatedResponse response = service.placeOrder(new OrderRequest(
                new BigDecimal("133"), Currency.USD, Currency.KRW));

        // 133 × 1403.58 = 186676.14 → floor → 186676
        assertThat(response.fromCurrency()).isEqualTo(Currency.USD);
        assertThat(response.toCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.fromAmount()).isEqualByComparingTo("133");
        assertThat(response.toAmount()).isEqualByComparingTo("186676");
        assertThat(response.tradeRate()).isEqualByComparingTo("1403.58");
    }

    @Test
    @DisplayName("저장되는_Order_엔티티는_OrderType이_BUY로_결정된다")
    void 저장되는_Order_엔티티는_OrderType이_BUY로_결정된다() {
        stubLatestRate(Currency.USD, "1477.45");

        service.placeOrder(new OrderRequest(
                new BigDecimal("200"), Currency.KRW, Currency.USD));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo(OrderType.BUY);
    }

    @Test
    @DisplayName("JPY_매수_1000엔은_100엔_단위_환율로_환산된다")
    void JPY_매수_1000엔은_100엔_단위_환율로_환산된다() {
        // 매매기준율 910.50 → buyRate = 956.03
        stubLatestRate(Currency.JPY, "910.50");

        OrderCreatedResponse response = service.placeOrder(new OrderRequest(
                new BigDecimal("1000"), Currency.KRW, Currency.JPY));

        // 1000 × 956.03 / 100 = 9560.30 → floor → 9560
        assertThat(response.fromAmount()).isEqualByComparingTo("9560");
        assertThat(response.toAmount()).isEqualByComparingTo("1000");
        assertThat(response.tradeRate()).isEqualByComparingTo("956.03");
    }

    @Test
    @DisplayName("JPY_100엔_미만_주문은_JPY_AMOUNT_BELOW_MIN_UNIT_BusinessException이다")
    void JPY_100엔_미만_주문은_JPY_AMOUNT_BELOW_MIN_UNIT_BusinessException이다() {
        // 환율 stub 도 필요 없음 — 검증에서 컷
        assertThatThrownBy(() -> service.placeOrder(new OrderRequest(
                new BigDecimal("50"), Currency.KRW, Currency.JPY)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(OrderErrorCode.JPY_AMOUNT_BELOW_MIN_UNIT);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("JPY_정확히_100엔은_허용된다")
    void JPY_정확히_100엔은_허용된다() {
        stubLatestRate(Currency.JPY, "910.50");

        OrderCreatedResponse response = service.placeOrder(new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.JPY));

        // 100 × 956.03 / 100 = 956.03 → floor → 956
        assertThat(response.fromAmount()).isEqualByComparingTo("956");
    }

    @Test
    @DisplayName("환율이_DB에_없으면_EXCHANGE_RATE_NOT_FOUND_BusinessException이다")
    void 환율이_DB에_없으면_EXCHANGE_RATE_NOT_FOUND_BusinessException이다() {
        when(exchangeRateRepository.findTopByCurrencyOrderByCollectedAtDesc(Currency.EUR))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeOrder(new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.EUR)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("외화_외화_조합은_INVALID_CURRENCY_PAIR로_거부된다")
    void 외화_외화_조합은_INVALID_CURRENCY_PAIR로_거부된다() {
        assertThatThrownBy(() -> service.placeOrder(new OrderRequest(
                new BigDecimal("100"), Currency.USD, Currency.JPY)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_CURRENCY_PAIR);

        verify(exchangeRateRepository, never()).findTopByCurrencyOrderByCollectedAtDesc(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("KRW_KRW_조합도_INVALID_CURRENCY_PAIR로_거부된다")
    void KRW_KRW_조합도_INVALID_CURRENCY_PAIR로_거부된다() {
        assertThatThrownBy(() -> service.placeOrder(new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.KRW)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_CURRENCY_PAIR);
    }

    @Test
    @DisplayName("CNY_매수_주문도_정상_처리된다")
    void CNY_매수_주문도_정상_처리된다() {
        stubLatestRate(Currency.CNY, "200.00");
        // buyRate = 200 × 1.05 = 210.00

        OrderCreatedResponse response = service.placeOrder(new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.CNY));

        // 100 × 210.00 = 21000.00 → floor → 21000
        assertThat(response.fromAmount()).isEqualByComparingTo("21000");
        assertThat(response.toAmount()).isEqualByComparingTo("100");
        assertThat(response.tradeRate()).isEqualByComparingTo("210.00");
    }

    private void stubLatestRate(Currency currency, String tradeStanRate) {
        ExchangeRateHistory history = ExchangeRateHistory.of(
                currency, new BigDecimal(tradeStanRate), LocalDateTime.now());
        when(exchangeRateRepository.findTopByCurrencyOrderByCollectedAtDesc(currency))
                .thenReturn(Optional.of(history));
    }
}
