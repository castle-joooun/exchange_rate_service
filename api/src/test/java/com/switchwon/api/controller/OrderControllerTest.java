package com.switchwon.api.controller;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST_order_KRW_USD_매수_요청은_OK_응답에_returnObject가_담겨_반환된다")
    void POST_order_KRW_USD_매수_요청은_OK_응답에_returnObject가_담겨_반환된다() throws Exception {
        when(orderService.placeOrder(any())).thenReturn(new OrderCreatedResponse(
                new BigDecimal("296086"), Currency.KRW,
                new BigDecimal("200"), Currency.USD,
                new BigDecimal("1480.43"),
                LocalDateTime.of(2026, 4, 28, 12, 0)
        ));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "forexAmount": 200, "fromCurrency": "KRW", "toCurrency": "USD" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.fromAmount").value(296086))
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.toAmount").value(200))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeRate").value(1480.43))
                .andExpect(jsonPath("$.returnObject.dateTime").value("2026-04-28T12:00:00"))
                .andExpect(jsonPath("$.returnObject.id").doesNotExist());
    }

    @Test
    @DisplayName("forexAmount가_0이면_400_BadRequest_응답이다")
    void forexAmount가_0이면_400_BadRequest_응답이다() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "forexAmount": 0, "fromCurrency": "KRW", "toCurrency": "USD" }
                                """))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any());
    }

    @Test
    @DisplayName("forexAmount가_음수면_400_BadRequest_응답이다")
    void forexAmount가_음수면_400_BadRequest_응답이다() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "forexAmount": -10, "fromCurrency": "KRW", "toCurrency": "USD" }
                                """))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any());
    }

    @Test
    @DisplayName("forexAmount_누락_시_400_BadRequest_응답이다")
    void forexAmount_누락_시_400_BadRequest_응답이다() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "fromCurrency": "KRW", "toCurrency": "USD" }
                                """))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any());
    }

    @Test
    @DisplayName("fromCurrency가_잘못된_enum이면_400_BadRequest_응답이다")
    void fromCurrency가_잘못된_enum이면_400_BadRequest_응답이다() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "forexAmount": 100, "fromCurrency": "XXX", "toCurrency": "USD" }
                                """))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any());
    }
}
