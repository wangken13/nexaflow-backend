package com.vebcoding.trade.customer.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.customer.api.CreateCustomerRequest;
import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.service.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<CustomerView>> list() {
        return ApiResponse.ok(customerService.list());
    }

    @PostMapping
    public ApiResponse<CustomerView> create(@RequestBody CreateCustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(customerService.tags());
    }
}