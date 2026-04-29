package com.switchwon.api.integration;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.domain.ExchangeRateHistory;
import com.switchwon.api.repository.ExchangeRateHistoryRepository;
import com.switchwon.external.service.ExchangeRateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 환율 조회 end-to-end 통합 테스트.
 *
 * <p>실제 Spring Context + 실제 H2 DB + 실제 Caffeine 캐시. 각 컴포넌트가 함께 동작하는지 검증.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExchangeRateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ExchangeRateHistoryRepository repository;
    @Autowired
    private CacheManager cacheManager;
    /** 통합 테스트는 외부 한국수출입은행 API 를 호출하지 않는다 (인터넷 의존성 제거). */
    @MockBean
    private ExchangeRateClient exchangeRateClient;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear());

        repository.save(ExchangeRateHistory.of(
                Currency.USD, new BigDecimal("1477.45"),
                LocalDateTime.of(2026, 4, 28, 12, 0)));
        repository.save(ExchangeRateHistory.of(
                Currency.JPY, new BigDecimal("910.50"),
                LocalDateTime.of(2026, 4, 28, 12, 0)));
    }

    @Test
    @DisplayName("GET_exchange_rate_latest_USD는_DB_저장값과_buy_sell_매핑까지_정확히_반환한다")
    void GET_exchange_rate_latest_USD는_DB_저장값과_buy_sell_매핑까지_정확히_반환한다() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeStanRate").value(1477.45))
                .andExpect(jsonPath("$.returnObject.buyRate").value(1551.32))
                .andExpect(jsonPath("$.returnObject.sellRate").value(1403.58))
                .andExpect(jsonPath("$.returnObject.dateTime").value("2026-04-28T12:00:00"));
    }

    @Test
    @DisplayName("GET_exchange_rate_latest는_2건을_returnObject_exchangeRateList로_반환한다")
    void GET_exchange_rate_latest는_2건을_returnObject_exchangeRateList로_반환한다() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.exchangeRateList.length()").value(2));
    }

    @Test
    @DisplayName("GET_exchange_rate_latest_USD_연속_호출_시_두번째는_캐시_hit이다")
    void GET_exchange_rate_latest_USD_연속_호출_시_두번째는_캐시_hit이다() throws Exception {
        // 1차 — 캐시 miss → DB 조회 → 캐시 저장
        mockMvc.perform(get("/exchange-rate/latest/USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // DB 의 row 를 임의로 바꿔도 (캐시가 살아있으므로) 응답엔 영향 없어야 함
        repository.deleteAll();

        // 2차 — 캐시 hit → DB 안 봄 → 1차 응답값 그대로
        mockMvc.perform(get("/exchange-rate/latest/USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.tradeStanRate").value(1477.45));
    }

    @Test
    @DisplayName("환율이_없는_통화_조회는_404_RATE_001_응답이다")
    void 환율이_없는_통화_조회는_404_RATE_001_응답이다() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/EUR").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATE_001"));
    }

    @Test
    @DisplayName("잘못된_통화_코드는_400_COMMON_001_응답이다")
    void 잘못된_통화_코드는_400_COMMON_001_응답이다() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/XXX").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("응답_헤더에_X_Trace_Id가_포함된다")
    void 응답_헤더에_X_Trace_Id가_포함된다() throws Exception {
        var result = mockMvc.perform(get("/exchange-rate/latest/USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Trace-Id")).isNotBlank();
    }
}
