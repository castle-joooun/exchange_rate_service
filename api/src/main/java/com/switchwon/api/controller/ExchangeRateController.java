package com.switchwon.api.controller;

import com.switchwon.api.domain.Currency;
import com.switchwon.api.dto.ExchangeRateListResponse;
import com.switchwon.api.dto.ExchangeRateResponse;
import com.switchwon.api.service.ExchangeRateService;
import com.switchwon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/exchange-rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/latest")
    public ApiResponse<ExchangeRateListResponse> getLatestAll() {
        return ApiResponse.success(exchangeRateService.getLatestAll());
    }

    @GetMapping("/latest/{currency}")
    public ApiResponse<ExchangeRateResponse> getLatest(@PathVariable Currency currency) {
        return ApiResponse.success(exchangeRateService.getLatest(currency));
    }
}
