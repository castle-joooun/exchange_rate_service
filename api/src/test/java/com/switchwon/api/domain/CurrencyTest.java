package com.switchwon.api.domain;

import com.switchwon.api.exception.DomainErrorCode;
import com.switchwon.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyTest {

    @Test
    @DisplayName("외부_API의_JPY_100_표기는_JPY로_매핑된다")
    void 외부_API의_JPY_100_표기는_JPY로_매핑된다() {
        assertThat(Currency.fromExternalCode("JPY(100)")).isEqualTo(Currency.JPY);
        assertThat(Currency.fromExternalCode("JPY")).isEqualTo(Currency.JPY);
    }

    @Test
    @DisplayName("외부_API의_CNH는_CNY로_매핑된다")
    void 외부_API의_CNH는_CNY로_매핑된다() {
        assertThat(Currency.fromExternalCode("CNH")).isEqualTo(Currency.CNY);
    }

    @Test
    @DisplayName("USD_EUR은_그대로_매핑된다")
    void USD_EUR은_그대로_매핑된다() {
        assertThat(Currency.fromExternalCode("USD")).isEqualTo(Currency.USD);
        assertThat(Currency.fromExternalCode("EUR")).isEqualTo(Currency.EUR);
    }

    @Test
    @DisplayName("지원하지_않는_통화는_UNSUPPORTED_CURRENCY_BusinessException이다")
    void 지원하지_않는_통화는_UNSUPPORTED_CURRENCY_BusinessException이다() {
        assertThatThrownBy(() -> Currency.fromExternalCode("GBP"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.UNSUPPORTED_CURRENCY);
    }

    @Test
    @DisplayName("null_통화_코드는_UNSUPPORTED_CURRENCY_BusinessException이다")
    void null_통화_코드는_UNSUPPORTED_CURRENCY_BusinessException이다() {
        assertThatThrownBy(() -> Currency.fromExternalCode(null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DomainErrorCode.UNSUPPORTED_CURRENCY);
    }

    @Test
    @DisplayName("KRW를_제외한_나머지는_isForex가_true다")
    void KRW를_제외한_나머지는_isForex가_true다() {
        assertThat(Currency.KRW.isForex()).isFalse();
        assertThat(Currency.USD.isForex()).isTrue();
        assertThat(Currency.JPY.isForex()).isTrue();
    }
}
