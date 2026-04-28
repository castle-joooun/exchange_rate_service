package com.switchwon.api.repository;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.Order;
import com.switchwon.api.domain.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Test
    @DisplayName("주문_조회는_생성일_기준_내림차순으로_정렬되며_각_주문의_모든_필드가_보존된다")
    void 주문_조회는_생성일_기준_내림차순으로_정렬되며_각_주문의_모든_필드가_보존된다() throws InterruptedException {
        Order first = repository.save(Order.create(
                new BigDecimal("296086"), Currency.KRW,
                new BigDecimal("200.00"), Currency.USD,
                new BigDecimal("1480.43")));
        Thread.sleep(10);
        Order second = repository.save(Order.create(
                new BigDecimal("133"), Currency.USD,
                new BigDecimal("196104"), Currency.KRW,
                new BigDecimal("1474.47")));

        List<Order> orders = repository.findAllByOrderByCreatedAtDesc();

        assertThat(orders).hasSize(2);

        Order latest = orders.get(0);
        assertThat(latest.getId()).isEqualTo(second.getId());
        assertThat(latest.getFromAmount()).isEqualByComparingTo("133");
        assertThat(latest.getFromCurrency()).isEqualTo(Currency.USD);
        assertThat(latest.getToAmount()).isEqualByComparingTo("196104");
        assertThat(latest.getToCurrency()).isEqualTo(Currency.KRW);
        assertThat(latest.getTradeRate()).isEqualByComparingTo("1474.47");
        assertThat(latest.getOrderType()).isEqualTo(OrderType.SELL);
        assertThat(latest.getCreatedAt()).isNotNull();

        Order older = orders.get(1);
        assertThat(older.getId()).isEqualTo(first.getId());
        assertThat(older.getFromAmount()).isEqualByComparingTo("296086");
        assertThat(older.getFromCurrency()).isEqualTo(Currency.KRW);
        assertThat(older.getToAmount()).isEqualByComparingTo("200.00");
        assertThat(older.getToCurrency()).isEqualTo(Currency.USD);
        assertThat(older.getTradeRate()).isEqualByComparingTo("1480.43");
        assertThat(older.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(older.getCreatedAt()).isNotNull();

        assertThat(latest.getCreatedAt()).isAfterOrEqualTo(older.getCreatedAt());
    }

    @Test
    @DisplayName("주문이_없으면_빈_리스트를_반환한다")
    void 주문이_없으면_빈_리스트를_반환한다() {
        List<Order> orders = repository.findAllByOrderByCreatedAtDesc();

        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("주문_저장_시_createdAt이_자동_채워진다")
    void 주문_저장_시_createdAt이_자동_채워진다() {
        Order saved = repository.save(Order.create(
                new BigDecimal("296086"), Currency.KRW,
                new BigDecimal("200.00"), Currency.USD,
                new BigDecimal("1480.43")));

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
