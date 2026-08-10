package com.vebcoding.trade.order.client;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.product.api.ProductView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", contextId = "orderProductClient")
public interface OrderProductClient {
    @GetMapping("/product/{id}")
    ApiResponse<ProductView> get(@PathVariable("id") String id);
}
