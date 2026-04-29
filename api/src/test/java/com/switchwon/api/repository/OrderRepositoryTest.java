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
    @DisplayName("주문_조회는_id_오름차순으로_정렬되며_각_주문의_모든_필드가_보존된다")
    void 주문_조회는_id_오름차순으로_정렬되며_각_주문의_모든_필드가_보존된다() {
        Order first = repository.save(Order.create(
                new BigDecimal("296086"), Currency.KRW,
                new BigDecimal("200.00"), Currency.USD,
                new BigDecimal("1480.43")));
        Order second = repository.save(Order.create(
                new BigDecimal("133"), Currency.USD,
                new BigDecimal("196104"), Currency.KRW,
                new BigDecimal("1474.47")));

        List<Order> orders = repository.findAllByOrderByIdAsc();

        assertThat(orders).hasSize(2);

        Order earliest = orders.get(0);
        assertThat(earliest.getId()).isEqualTo(first.getId());
        assertThat(earliest.getFromAmount()).isEqualByComparingTo("296086");
        assertThat(earliest.getFromCurrency()).isEqualTo(Currency.KRW);
        assertThat(earliest.getToAmount()).isEqualByComparingTo("200.00");
        assertThat(earliest.getToCurrency()).isEqualTo(Currency.USD);
        assertThat(earliest.getTradeRate()).isEqualByComparingTo("1480.43");
        assertThat(earliest.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(earliest.getCreatedAt()).isNotNull();

        Order later = orders.get(1);
        assertThat(later.getId()).isEqualTo(second.getId());
        assertThat(later.getFromAmount()).isEqualByComparingTo("133");
        assertThat(later.getFromCurrency()).isEqualTo(Currency.USD);
        assertThat(later.getToAmount()).isEqualByComparingTo("196104");
        assertThat(later.getToCurrency()).isEqualTo(Currency.KRW);
        assertThat(later.getTradeRate()).isEqualByComparingTo("1474.47");
        assertThat(later.getOrderType()).isEqualTo(OrderType.SELL);
        assertThat(later.getCreatedAt()).isNotNull();

        assertThat(earliest.getId()).isLessThan(later.getId());
    }

    @Test
    @DisplayName("주문이_없으면_빈_리스트를_반환한다")
    void 주문이_없으면_빈_리스트를_반환한다() {
        List<Order> orders = repository.findAllByOrderByIdAsc();

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
