package com.vebcoding.trade.order.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.order.api.CreateOrderRequest;
import com.vebcoding.trade.order.api.OrderView;
import com.vebcoding.trade.order.service.OrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderView>> list() {
        return ApiResponse.ok(orderService.list());
    }

    @GetMapping("/exceptions")
    public ApiResponse<List<OrderView>> exceptions() {
        return ApiResponse.ok(orderService.exceptions());
    }

    @PostMapping
    public ApiResponse<OrderView> create(@RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }
}