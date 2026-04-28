package com.switchwon.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("KRW에서_USD로_생성하면_모든_필드가_요청대로_채워지고_BUY_타입이_된다")
    void KRW에서_USD로_생성하면_모든_필드가_요청대로_채워지고_BUY_타입이_된다() {
        BigDecimal fromAmount = new BigDecimal("296086");
        BigDecimal toAmount = new BigDecimal("200.00");
        BigDecimal tradeRate = new BigDecimal("1480.43");

        Order order = Order.create(fromAmount, Currency.KRW, toAmount, Currency.USD, tradeRate);

        assertThat(order.getFromAmount()).isEqualByComparingTo(fromAmount);
        assertThat(order.getFromCurrency()).isEqualTo(Currency.KRW);
        assertThat(order.getToAmount()).isEqualByComparingTo(toAmount);
        assertThat(order.getToCurrency()).isEqualTo(Currency.USD);
        assertThat(order.getTradeRate()).isEqualByComparingTo(tradeRate);
        assertThat(order.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(order.getId()).isNull();
        assertThat(order.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("USD에서_KRW로_생성하면_모든_필드가_요청대로_채워지고_SELL_타입이_된다")
    void USD에서_KRW로_생성하면_모든_필드가_요청대로_채워지고_SELL_타입이_된다() {
        BigDecimal fromAmount = new BigDecimal("133");
        BigDecimal toAmount = new BigDecimal("196104");
        BigDecimal tradeRate = new BigDecimal("1474.47");

        Order order = Order.create(fromAmount, Currency.USD, toAmount, Currency.KRW, tradeRate);

        assertThat(order.getFromAmount()).isEqualByComparingTo(fromAmount);
        assertThat(order.getFromCurrency()).isEqualTo(Currency.USD);
        assertThat(order.getToAmount()).isEqualByComparingTo(toAmount);
        assertThat(order.getToCurrency()).isEqualTo(Currency.KRW);
        assertThat(order.getTradeRate()).isEqualByComparingTo(tradeRate);
        assertThat(order.getOrderType()).isEqualTo(OrderType.SELL);
    }

    @Test
    @DisplayName("JPY_매수_주문도_적용_환율과_OrderType_BUY가_그대로_보존된다")
    void JPY_매수_주문도_적용_환율과_OrderType_BUY가_그대로_보존된다() {
        Order order = Order.create(
                new BigDecimal("100000"),
                Currency.KRW,
                new BigDecimal("10000"),
                Currency.JPY,
                new BigDecimal("955.00")
        );

        assertThat(order.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(order.getToCurrency()).isEqualTo(Currency.JPY);
        assertThat(order.getTradeRate()).isEqualByComparingTo("955.00");
    }
}
