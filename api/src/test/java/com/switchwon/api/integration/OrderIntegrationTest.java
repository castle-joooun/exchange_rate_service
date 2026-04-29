package com.switchwon.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.dto.OrderRequest;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.api.repository.OrderRepository;
import com.switchwon.external.service.ExchangeRateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주문 end-to-end 통합 테스트.
 *
 * <p>실제 Spring Context + 실제 H2 DB + 실제 트랜잭션. 단위 테스트가 커버하지 못하는
 * "여러 컴포넌트가 함께 동작하는지" 를 검증한다.</p>
 *
 * <p>외부 API 는 호출하지 않고, 환율 row 를 직접 DB 에 시드한 뒤 흐름을 검증한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ExchangeRateHistoryRepository exchangeRateRepository;
    @Autowired
    private OrderRepository orderRepository;
    /** 통합 테스트는 외부 한국수출입은행 API 를 호출하지 않는다 (인터넷 의존성 제거). */
    @MockBean
    private ExchangeRateClient exchangeRateClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        exchangeRateRepository.deleteAll();

        // 환율 시드 (USD 매매기준율 1477.45 → buy 1551.32, sell 1403.58)
        exchangeRateRepository.save(ExchangeRateHistory.of(
                Currency.USD, new BigDecimal("1477.45"),
                LocalDateTime.of(2026, 4, 28, 12, 0)));
    }

    @Test
    @DisplayName("KRW에서_USD_매수_주문이_DB에_저장되고_조회_API에_노출된다")
    void KRW에서_USD_매수_주문이_DB에_저장되고_조회_API에_노출된다() throws Exception {
        // 1. 주문 생성
        OrderRequest request = new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.USD);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                // 100 × buyRate(1551.32) = 155132.00 → floor → 155132
                .andExpect(jsonPath("$.returnObject.fromAmount").value(155132))
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.toAmount").value(100))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeRate").value(1551.32))
                .andExpect(jsonPath("$.returnObject.id").doesNotExist());

        // 2. 주문 조회로 확인
        mockMvc.perform(get("/order/list").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.orderList.length()").value(1))
                .andExpect(jsonPath("$.returnObject.orderList[0].id").exists())
                .andExpect(jsonPath("$.returnObject.orderList[0].fromAmount").value(155132))
                .andExpect(jsonPath("$.returnObject.orderList[0].toCurrency").value("USD"));
    }

    @Test
    @DisplayName("USD에서_KRW_매도_주문은_sellRate가_적용되어_저장된다")
    void USD에서_KRW_매도_주문은_sellRate가_적용되어_저장된다() throws Exception {
        OrderRequest request = new OrderRequest(
                new BigDecimal("100"), Currency.USD, Currency.KRW);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // 100 × sellRate(1403.58) = 140358.00 → 140358
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.toAmount").value(140358))
                .andExpect(jsonPath("$.returnObject.tradeRate").value(1403.58));
    }

    @Test
    @DisplayName("환율이_없는_통화로_주문하면_404_RATE_001_응답이다")
    void 환율이_없는_통화로_주문하면_404_RATE_001_응답이다() throws Exception {
        OrderRequest request = new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.EUR);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATE_001"));
    }

    @Test
    @DisplayName("외화_외화_주문은_400_DOMAIN_002_응답이다")
    void 외화_외화_주문은_400_DOMAIN_002_응답이다() throws Exception {
        OrderRequest request = new OrderRequest(
                new BigDecimal("100"), Currency.USD, Currency.JPY);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOMAIN_002"));
    }

    @Test
    @DisplayName("두건_연속_주문이_id_오름차순으로_조회된다")
    void 두건_연속_주문이_id_오름차순으로_조회된다() throws Exception {
        OrderRequest first = new OrderRequest(
                new BigDecimal("100"), Currency.KRW, Currency.USD);
        OrderRequest second = new OrderRequest(
                new BigDecimal("50"), Currency.USD, Currency.KRW);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)));
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)));

        mockMvc.perform(get("/order/list").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.orderList.length()").value(2))
                .andExpect(jsonPath("$.returnObject.orderList[0].fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.orderList[1].fromCurrency").value("USD"));
    }
}
