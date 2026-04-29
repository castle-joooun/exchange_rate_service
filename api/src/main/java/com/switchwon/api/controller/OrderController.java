package com.switchwon.api.controller;

import com.switchwon.api.dto.OrderCreatedResponse;
import com.switchwon.api.dto.OrderRequest;
import com.switchwon.api.service.OrderService;
import com.switchwon.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderCreatedResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.success(orderService.placeOrder(request));
    }
}
