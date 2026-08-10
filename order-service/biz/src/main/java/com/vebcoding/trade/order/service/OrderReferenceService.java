package com.vebcoding.trade.order.service;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.customer.api.CustomerDetailView;
import com.vebcoding.trade.order.client.OrderCustomerClient;
import com.vebcoding.trade.order.client.OrderProductClient;
import com.vebcoding.trade.product.api.ProductView;
import org.springframework.stereotype.Service;

@Service
public class OrderReferenceService {
    private final OrderCustomerClient customerClient;
    private final OrderProductClient productClient;

    public OrderReferenceService(OrderCustomerClient customerClient, OrderProductClient productClient) {
        this.customerClient = customerClient;
        this.productClient = productClient;
    }

    public OrderReference resolve(String customerId, String productId) {
        try {
            ApiResponse<CustomerDetailView> customerResponse = customerClient.detail(customerId);
            ApiResponse<ProductView> productResponse = productClient.get(productId);
            if (!customerResponse.success() || customerResponse.data() == null) throw BusinessException.notFound("所选客户不存在");
            if (!productResponse.success() || productResponse.data() == null) throw BusinessException.notFound("所选产品不存在");
            if (!productResponse.data().active()) throw new BusinessException("所选产品已停用");
            return new OrderReference(customerResponse.data().customer().id(), customerResponse.data().customer().name(),
                    productResponse.data().id(), productResponse.data().name());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw BusinessException.unavailable("客户或产品服务暂不可用，请稍后重试");
        }
    }

    public record OrderReference(String customerId, String customerName, String productId, String productName) {
    }
}
