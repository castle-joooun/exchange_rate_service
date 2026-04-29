package com.switchwon.external.service;

import com.switchwon.common.exception.BusinessException;
import com.switchwon.external.config.KoreaeximProperties;
import com.switchwon.external.dto.DailyExchangeRate;
import com.switchwon.external.exception.ExternalErrorCode;
import com.switchwon.external.handler.KoreaeximResponseErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class KoreaeximExchangeRateClientTest {

    private static final String BASE_URL = "https://oapi.koreaexim.go.kr";
    private static final String AUTH_KEY = "test-auth-key";

    private RestClient restClient;
    private MockRestServiceServer server;
    private KoreaeximProperties properties;
    private ExchangeRateFallbackProvider fallbackProvider;
    private KoreaeximExchangeRateClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultStatusHandler(new KoreaeximResponseErrorHandler());
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        properties = new KoreaeximProperties(BASE_URL, AUTH_KEY, 3000, 5000, List.of(12, 15, 18, 21));
        fallbackProvider = mock(ExchangeRateFallbackProvider.class);
        client = new KoreaeximExchangeRateClient(restClient, properties, fallbackProvider);
    }

    @Test
    @DisplayName("정상_응답은_외부_currency_코드와_매매기준율이_그대로_보존된다")
    void 정상_응답은_외부_currency_코드와_매매기준율이_그대로_보존된다() {
        String body = """
                [
                  {"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1474.47","tts":"1480.43","deal_bas_r":"1477.45"},
                  {"result":1,"cur_unit":"JPY(100)","cur_nm":"일본 옌","ttb":"905.80","tts":"915.20","deal_bas_r":"910.50"},
                  {"result":1,"cur_unit":"CNH","cur_nm":"위안화","ttb":"200.00","tts":"205.00","deal_bas_r":"202.50"},
                  {"result":1,"cur_unit":"EUR","cur_nm":"유로","ttb":"1580.00","tts":"1620.00","deal_bas_r":"1600.00"}
                ]
                """;

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andExpect(method(GET))
                .andExpect(queryParam("authkey", AUTH_KEY))
                .andExpect(queryParam("searchdate", "20260428"))
                .andExpect(queryParam("data", "AP01"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<DailyExchangeRate> result = client.fetchDailyRates(LocalDate.of(2026, 4, 28));

        assertThat(result).hasSize(4);
        assertThat(result).extracting(DailyExchangeRate::currencyCode)
                .containsExactlyInAnyOrder("USD", "JPY(100)", "CNH", "EUR");
        assertThat(result).filteredOn(r -> r.currencyCode().equals("USD"))
                .singleElement()
                .extracting(DailyExchangeRate::tradeStanRate)
                .satisfies(rate -> assertThat(rate).isEqualByComparingTo("1477.45"));
    }

    @Test
    @DisplayName("응답에_포함된_미지원_통화도_필터링하지_않고_그대로_반환된다")
    void 응답에_포함된_미지원_통화도_필터링하지_않고_그대로_반환된다() {
        // 매핑/필터링 책임은 api 모듈로 이동했으므로 external 은 통화를 가르지 않는다.
        String body = """
                [
                  {"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1474.47","tts":"1480.43","deal_bas_r":"1477.45"},
                  {"result":1,"cur_unit":"GBP","cur_nm":"영국 파운드","ttb":"1800.00","tts":"1830.00","deal_bas_r":"1815.00"}
                ]
                """;

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<DailyExchangeRate> result = client.fetchDailyRates(LocalDate.of(2026, 4, 28));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DailyExchangeRate::currencyCode)
                .containsExactlyInAnyOrder("USD", "GBP");
    }

    @Test
    @DisplayName("응답이_빈_배열이면_빈_리스트를_반환한다")
    void 응답이_빈_배열이면_빈_리스트를_반환한다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<DailyExchangeRate> result = client.fetchDailyRates(LocalDate.of(2026, 4, 28));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("환율_파싱_실패_시_EXTERNAL_API_BAD_RESPONSE_BusinessException이다")
    void 환율_파싱_실패_시_EXTERNAL_API_BAD_RESPONSE_BusinessException이다() {
        String body = """
                [{"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1474.47","tts":"1480.43","deal_bas_r":"NOT_A_NUMBER"}]
                """;

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchDailyRates(LocalDate.of(2026, 4, 28)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_BAD_RESPONSE);
    }

    @Test
    @DisplayName("응답_result가_1이_아니면_EXTERNAL_API_BAD_RESPONSE_BusinessException이다")
    void 응답_result가_1이_아니면_EXTERNAL_API_BAD_RESPONSE_BusinessException이다() {
        String body = """
                [{"result":3,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1474.47","tts":"1480.43","deal_bas_r":"1477.45"}]
                """;

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchDailyRates(LocalDate.of(2026, 4, 28)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_BAD_RESPONSE);
    }

    @Test
    @DisplayName("HTTP_500_응답은_ResponseErrorHandler가_BusinessException으로_변환한다")
    void HTTP_500_응답은_ResponseErrorHandler가_BusinessException으로_변환한다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.fetchDailyRates(LocalDate.of(2026, 4, 28)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("Fallback_메서드는_FallbackProvider의_랜덤_데이터를_그대로_반환한다")
    void Fallback_메서드는_FallbackProvider의_랜덤_데이터를_그대로_반환한다() throws Exception {
        // CircuitBreaker AOP 없이 fallback 자체를 직접 호출 (단위 검증)
        var fallbackMethod = KoreaeximExchangeRateClient.class.getDeclaredMethod(
                "fallback", LocalDate.class, Throwable.class);
        fallbackMethod.setAccessible(true);

        when(fallbackProvider.getLatestAsRandomized()).thenReturn(List.of(
                new DailyExchangeRate("USD", new java.math.BigDecimal("1480.00"))
        ));

        @SuppressWarnings("unchecked")
        List<DailyExchangeRate> result = (List<DailyExchangeRate>) fallbackMethod.invoke(
                client, LocalDate.of(2026, 4, 28), new RuntimeException("boom"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currencyCode()).isEqualTo("USD");
        verify(fallbackProvider, times(1)).getLatestAsRandomized();
    }

    @Test
    @DisplayName("FallbackProvider가_빈_리스트를_반환하면_fallback도_빈_리스트를_반환한다")
    void FallbackProvider가_빈_리스트를_반환하면_fallback도_빈_리스트를_반환한다() throws Exception {
        var fallbackMethod = KoreaeximExchangeRateClient.class.getDeclaredMethod(
                "fallback", LocalDate.class, Throwable.class);
        fallbackMethod.setAccessible(true);

        when(fallbackProvider.getLatestAsRandomized()).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<DailyExchangeRate> result = (List<DailyExchangeRate>) fallbackMethod.invoke(
                client, LocalDate.of(2026, 4, 28), new RuntimeException("boom"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("HTTP_400_응답도_BusinessException_EXTERNAL_API_UNAVAILABLE로_변환된다")
    void HTTP_400_응답도_BusinessException_EXTERNAL_API_UNAVAILABLE로_변환된다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.fetchDailyRates(LocalDate.of(2026, 4, 28)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("HTTP_401_응답은_BusinessException_EXTERNAL_API_AUTH_FAILED로_변환된다")
    void HTTP_401_응답은_BusinessException_EXTERNAL_API_AUTH_FAILED로_변환된다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchDailyRates(LocalDate.of(2026, 4, 28)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ExternalErrorCode.EXTERNAL_API_AUTH_FAILED);
    }

    @Test
    @DisplayName("환율값이_콤마를_포함해도_정상_파싱된다")
    void 환율값이_콤마를_포함해도_정상_파싱된다() {
        String body = """
                [{"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1,474.47","tts":"1,480.43","deal_bas_r":"1,477.45"}]
                """;

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<DailyExchangeRate> result = client.fetchDailyRates(LocalDate.of(2026, 4, 28));

        assertThat(result).singleElement()
                .extracting(DailyExchangeRate::tradeStanRate)
                .satisfies(rate -> assertThat(rate).isEqualByComparingTo("1477.45"));
    }

    @Test
    @DisplayName("정상_응답_시에는_FallbackProvider가_호출되지_않는다")
    void 정상_응답_시에는_FallbackProvider가_호출되지_않는다() {
        String body = """
                [{"result":1,"cur_unit":"USD","cur_nm":"미국 달러","ttb":"1474.47","tts":"1480.43","deal_bas_r":"1477.45"}]
                """;

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/site/program/financial/exchangeJSON")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        client.fetchDailyRates(LocalDate.of(2026, 4, 28));

        verify(fallbackProvider, org.mockito.Mockito.never()).getLatestAsRandomized();
    }
}
