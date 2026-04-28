package com.switchwon.api.domain;

import com.switchwon.common.log.LoggingPatterns;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 외화 주문 내역. 주문 시점의 적용 환율과 환산 금액을 함께 저장한다.
 * 환율은 매분 변동하므로 "주문 시점"의 스냅샷 보존이 핵심 설계 의도다.
 */
@Slf4j
@Getter
@Entity
@Table(
        name = "orders",
        indexes = @Index(name = "idx_orders_created_at", columnList = "created_at DESC")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal fromAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false, length = 3)
    private Currency fromCurrency;

    @Column(name = "to_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal toAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false, length = 3)
    private Currency toCurrency;

    @Column(name = "trade_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal tradeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Order(BigDecimal fromAmount,
                  Currency fromCurrency,
                  BigDecimal toAmount,
                  Currency toCurrency,
                  BigDecimal tradeRate,
                  OrderType orderType) {
        this.fromAmount = fromAmount;
        this.fromCurrency = fromCurrency;
        this.toAmount = toAmount;
        this.toCurrency = toCurrency;
        this.tradeRate = tradeRate;
        this.orderType = orderType;
    }

    public static Order create(BigDecimal fromAmount,
                               Currency fromCurrency,
                               BigDecimal toAmount,
                               Currency toCurrency,
                               BigDecimal tradeRate) {
        OrderType orderType = OrderType.resolve(fromCurrency, toCurrency);
        log.info("{} 주문 엔티티 생성 type={} from={}({}) to={}({}) tradeRate={}",
                LoggingPatterns.BIZ_EVENT,
                orderType,
                fromAmount, fromCurrency,
                toAmount, toCurrency,
                tradeRate);
        return new Order(fromAmount, fromCurrency, toAmount, toCurrency, tradeRate, orderType);
    }
}
