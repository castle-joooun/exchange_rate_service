package com.switchwon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * GET /order/list 응답 래퍼.
 *
 * <p>과제 스펙:</p>
 * <pre>
 * {
 *   "orderList": [
 *     { "id": 1, "fromAmount": 296086, "fromCurrency": "KRW", ... },
 *     ...
 *   ]
 * }
 * </pre>
 */
@Schema(description = "주문 내역 조회 응답")
public record OrderListResponse(
        @Schema(description = "최신순으로 정렬된 주문 목록")
        List<OrderListItemResponse> orderList
) {

    public static OrderListResponse of(List<OrderListItemResponse> orderList) {
        return new OrderListResponse(orderList);
    }
}
