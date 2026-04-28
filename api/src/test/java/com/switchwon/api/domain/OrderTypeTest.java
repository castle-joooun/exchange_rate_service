package com.switchwon.api.domain;

import com.switchwon.api.exception.DomainErrorCode;
import com.switchwon.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTypeTest {

    @Test
    @DisplayName("KRW에서_외화로의_주문은_BUY다")
    void KRW에서_외화로의_주문은_BUY다() {
        assertThat(OrderType.resolve(Currency.KRW, Currency.USD)).isEqualTo(OrderType.BUY);
    }

    @Test
    @DisplayName("외화에서_KRW로의_주문은_SELL이다")
    void 외화에서_KRW로의_주문은_SELL이다() {
        assertThat(OrderType.resolve(Currency.USD, Currency.KRW)).isEqualTo(OrderType.SELL);
    }

    @Test
    @DisplayName("외화_외화_조합은_INVALID_CURRENCY_PAIR_BusinessException이다")
    void 외화_외화_조합은_INVALID_CURRENCY_PAIR_BusinessException이다() {
        assertThatThrownBy(() -> OrderType.resolve(Currency.USD, Currency.JPY))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_CURRENCY_PAIR);
    }

    @Test
    @DisplayName("KRW_KRW_조합도_INVALID_CURRENCY_PAIR_BusinessException이다")
    void KRW_KRW_조합도_INVALID_CURRENCY_PAIR_BusinessException이다() {
        assertThatThrownBy(() -> OrderType.resolve(Currency.KRW, Currency.KRW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_CURRENCY_PAIR);
    }

    @Test
    @DisplayName("null_통화가_포함되면_INVALID_CURRENCY_PAIR_BusinessException이다")
    void null_통화가_포함되면_INVALID_CURRENCY_PAIR_BusinessException이다() {
        assertThatThrownBy(() -> OrderType.resolve(null, Currency.USD))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.INVALID_CURRENCY_PAIR);
    }
}
