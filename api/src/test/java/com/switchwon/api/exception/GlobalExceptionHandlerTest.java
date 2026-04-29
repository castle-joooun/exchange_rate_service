package com.switchwon.api.exception;

import com.switchwon.api.controller.ExchangeRateController;
import com.switchwon.api.domain.Currency;
import com.switchwon.api.service.ExchangeRateService;
import com.switchwon.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 가 각 예외를 의도한 응답으로 변환하는지 검증.
 * ExchangeRateController 를 가짜 컨트롤러로 활용한다.
 */
@WebMvcTest(controllers = ExchangeRateController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("BusinessException은_ErrorCode_HttpStatus와_code_message가_담긴_JSON으로_변환된다")
    void BusinessException은_ErrorCode_HttpStatus와_code_message가_담긴_JSON으로_변환된다() throws Exception {
        when(exchangeRateService.getLatest(Currency.EUR))
                .thenThrow(new BusinessException(
                        ExchangeRateErrorCode.EXCHANGE_RATE_NOT_FOUND,
                        "환율 정보 없음: currency=EUR"));

        mockMvc.perform(get("/exchange-rate/latest/EUR").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATE_001"))
                .andExpect(jsonPath("$.message").value("환율 정보 없음: currency=EUR"));
    }

    @Test
    @DisplayName("PathVariable_enum_변환_실패는_400_INVALID_DATA로_변환된다")
    void PathVariable_enum_변환_실패는_400_INVALID_DATA로_변환된다() throws Exception {
        mockMvc.perform(get("/exchange-rate/latest/INVALID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INVALID")));
    }

    @Test
    @DisplayName("예상치_못한_RuntimeException은_500_INTERNAL_ERROR로_변환된다")
    void 예상치_못한_RuntimeException은_500_INTERNAL_ERROR로_변환된다() throws Exception {
        when(exchangeRateService.getLatest(Currency.USD))
                .thenThrow(new RuntimeException("simulated"));

        mockMvc.perform(get("/exchange-rate/latest/USD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_999"))
                .andExpect(jsonPath("$.message").value("내부 오류가 발생했습니다"));
    }

    // traceId 필드 검증은 MdcFilter 가 동작하는 통합 테스트(#11) 에서 다룬다.
    // WebMvcTest 슬라이스에는 필터가 자동 등록되지 않아 traceId 가 항상 null 이다.
}
