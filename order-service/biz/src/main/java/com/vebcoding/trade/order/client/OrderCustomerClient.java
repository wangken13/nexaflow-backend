package com.vebcoding.trade.order.client;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.customer.api.CustomerDetailView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", contextId = "orderCustomerClient")
public interface OrderCustomerClient {
    @GetMapping("/customer/{id}")
    ApiResponse<CustomerDetailView> detail(@PathVariable("id") String id);
}
