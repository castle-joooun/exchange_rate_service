package com.switchwon.api.controller;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.dto.OrderListItemResponse;
import com.switchwon.api.dto.OrderListResponse;
import com.switchwon.api.exception.GlobalExceptionHandler;
import com.switchwon.api.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
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
    @DisplayName("forexAmount가_0이면_400_COMMON_001_JSON_응답이다")
    void forexAmount가_0이면_400_COMMON_001_JSON_응답이다() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "forexAmount": 0, "fromCurrency": "KRW", "toCurrency": "USD" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("forexAmount")));

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

    @Test
    @DisplayName("GET_order_list는_id_오름차순_orderList를_returnObject로_감싸_반환한다")
    void GET_order_list는_id_오름차순_orderList를_returnObject로_감싸_반환한다() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 4, 28, 12, 0);
        when(orderService.getOrderList()).thenReturn(new OrderListResponse(List.of(
                new OrderListItemResponse(1L,
                        new BigDecimal("296086"), Currency.KRW,
                        new BigDecimal("200"), Currency.USD,
                        new BigDecimal("1480.43"), at),
                new OrderListItemResponse(2L,
                        new BigDecimal("133"), Currency.USD,
                        new BigDecimal("196104"), Currency.KRW,
                        new BigDecimal("1474.47"), at)
        )));

        mockMvc.perform(get("/order/list").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.orderList.length()").value(2))
                .andExpect(jsonPath("$.returnObject.orderList[0].id").value(1))
                .andExpect(jsonPath("$.returnObject.orderList[0].fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.orderList[0].toAmount").value(200))
                .andExpect(jsonPath("$.returnObject.orderList[1].id").value(2))
                .andExpect(jsonPath("$.returnObject.orderList[1].fromCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.orderList[1].toAmount").value(196104));
    }

    @Test
    @DisplayName("GET_order_list_빈_리스트도_정상_200_응답이다")
    void GET_order_list_빈_리스트도_정상_200_응답이다() throws Exception {
        when(orderService.getOrderList()).thenReturn(new OrderListResponse(List.of()));

        mockMvc.perform(get("/order/list").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.orderList.length()").value(0));
    }
}
