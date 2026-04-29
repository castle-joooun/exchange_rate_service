package com.switchwon.api.controller;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.dto.ExchangeRateListResponse;
import com.switchwon.api.dto.ExchangeRateResponse;
import com.switchwon.api.exception.ExchangeRateErrorCode;
import com.switchwon.api.service.ExchangeRateService;
import com.switchwon.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("GET_exchange_rate_latest는_4건의_환율을_returnObject로_감싸_반환한다")
    void GET_exchange_rate_latest는_4건의_환율을_returnObject로_감싸_반환한다() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 4, 28, 12, 0);
        when(exchangeRateService.getLatestAll()).thenReturn(new ExchangeRateListResponse(List.of(
                new ExchangeRateResponse(Currency.USD, new BigDecimal("1551.32"),
                        new BigDecimal("1477.45"), new BigDecimal("1403.58"), at),
                new ExchangeRateResponse(Currency.JPY, new BigDecimal("956.03"),
                        new BigDecimal("910.50"), new BigDecimal("864.98"), at)
        )));

        mockMvc.perform(get("/exchange-rate/latest").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("성공"))
                .andExpect(jsonPath("$.returnObject.exchangeRateList.length()").value(2))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].buyRate").value(1551.32))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].tradeStanRate").value(1477.45))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].sellRate").value(1403.58));
    }

    @Test
    @DisplayName("GET_exchange_rate_latest_USD는_단일_통화_상세를_반환한다")
    void GET_exchange_rate_latest_USD는_단일_통화_상세를_반환한다() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 4, 28, 12, 0);
        when(exchangeRateService.getLatest(Currency.USD)).thenReturn(
                new ExchangeRateResponse(Currency.USD, new BigDecimal("1551.32"),
                        new BigDecimal("1477.45"), new BigDecimal("1403.58"), at));

        mockMvc.perform(get("/exchange-rate/latest/USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeStanRate").value(1477.45))
                .andExpect(jsonPath("$.returnObject.dateTime").value("2026-04-28T12:00:00"));
    }

    @Test
    @DisplayName("GET_exchange_rate_latest_KRW는_BusinessException을_그대로_propagate한다")
    void GET_exchange_rate_latest_KRW는_BusinessException을_그대로_propagate한다() throws Exception {
        // Issue #9 의 GlobalExceptionHandler 가 없으므로 컨트롤러는 예외를 던지는 데까지가 책임.
        // 응답 변환은 #9 에서 검증한다.
        when(exchangeRateService.getLatest(Currency.KRW))
                .thenThrow(new BusinessException(
                        ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND, "환율 정보 없음: currency=KRW"));

        assertThatThrownBy(() ->
                mockMvc.perform(get("/exchange-rate/latest/KRW").accept(MediaType.APPLICATION_JSON)))
                .hasRootCauseInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("잘못된_통화_코드는_400_Bad_Request를_반환한다")
    void 잘못된_통화_코드는_400_Bad_Request를_반환한다() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/INVALID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
