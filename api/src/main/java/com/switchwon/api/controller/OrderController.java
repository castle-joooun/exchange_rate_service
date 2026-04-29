package com.switchwon.api.controller;

import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.dto.OrderListResponse;
import com.switchwon.api.dto.OrderRequest;
import com.switchwon.api.service.OrderService;
import com.switchwon.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문", description = "외화 매수/매도 주문 + 주문 내역 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "외화 매수/매도 주문",
            description = """
                    KRW ↔ 외화 환전 주문을 처리한다.

                    - 매수 (KRW → 외화): buyRate 적용. fromAmount 가 자동 계산되어 KRW 출금 금액
                    - 매도 (외화 → KRW): sellRate 적용. toAmount 가 KRW 입금 금액
                    - JPY 는 100엔 단위 환율 사용. 100엔 미만 주문은 거부 (ORDER_001)
                    - forexAmount 는 항상 외화 기준 금액
                    """
    )
    @PostMapping
    public ApiResponse<OrderCreatedResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.success(orderService.placeOrder(request));
    }

    @Operation(
            summary = "주문 내역 조회",
            description = "전체 주문 내역을 최신순(생성일 DESC)으로 반환한다."
    )
    @GetMapping("/list")
    public ApiResponse<OrderListResponse> getOrderList() {
        return ApiResponse.success(orderService.getOrderList());
    }
}
