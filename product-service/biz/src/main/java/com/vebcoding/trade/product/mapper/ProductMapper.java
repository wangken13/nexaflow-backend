package com.vebcoding.trade.product.mapper;

import com.vebcoding.trade.product.api.ProductView;
import java.util.List;
import java.util.Optional;

public interface ProductMapper {
    List<ProductView> findByTenantId(String tenantId, String keyword);
    Optional<ProductView> findByTenantIdAndId(String tenantId, String id);
    ProductView save(ProductView product);
    boolean delete(String tenantId, String id);
}
