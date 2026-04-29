package com.switchwon.api.controller;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.dto.ExchangeRateListResponse;
import com.switchwon.api.dto.ExchangeRateResponse;
import com.switchwon.api.service.ExchangeRateService;
import com.switchwon.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "환율", description = "최신 환율 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/exchange-rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(
            summary = "전체 통화 최신 환율 조회",
            description = "USD / JPY / CNY / EUR 4개 통화의 가장 최근 매매기준율, 매입율, 매도율을 반환한다. " +
                    "60초 TTL 의 Caffeine 캐시가 적용되어 있다."
    )
    @GetMapping("/latest")
    public ApiResponse<ExchangeRateListResponse> getLatestAll() {
        return ApiResponse.success(exchangeRateService.getLatestAll());
    }

    @Operation(
            summary = "특정 통화 최신 환율 조회",
            description = "지정한 외화의 최신 환율을 반환한다. 환율 데이터가 없으면 404."
    )
    @GetMapping("/latest/{currency}")
    public ApiResponse<ExchangeRateResponse> getLatest(
            @Parameter(description = "조회할 외화 코드 (USD/JPY/CNY/EUR)", example = "USD")
            @PathVariable Currency currency) {
        return ApiResponse.success(exchangeRateService.getLatest(currency));
    }
}
