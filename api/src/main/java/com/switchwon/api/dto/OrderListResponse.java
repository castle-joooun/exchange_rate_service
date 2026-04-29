package com.switchwon.api.dto;

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
public record OrderListResponse(
        List<OrderListItemResponse> orderList
) {

    public static OrderListResponse of(List<OrderListItemResponse> orderList) {
        return new OrderListResponse(orderList);
    }
}
