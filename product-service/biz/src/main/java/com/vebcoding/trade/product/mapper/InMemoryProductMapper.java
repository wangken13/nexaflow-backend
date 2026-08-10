package com.vebcoding.trade.product.mapper;

import com.vebcoding.trade.product.api.ProductView;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryProductMapper implements ProductMapper {
    private final List<ProductView> products = new CopyOnWriteArrayList<>();
    public List<ProductView> findByTenantId(String tenantId, String keyword) {
        return products.stream().filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> item.sku().contains(keyword) || item.name().contains(keyword)).toList();
    }
    public Optional<ProductView> findByTenantIdAndId(String tenantId, String id) {
        return products.stream().filter(item -> tenantId.equals(item.tenantId()) && id.equals(item.id())).findFirst();
    }
    public ProductView save(ProductView product) {
        products.removeIf(item -> item.id().equals(product.id())); products.add(product); return product;
    }
    public boolean delete(String tenantId, String id) {
        return products.removeIf(item -> tenantId.equals(item.tenantId()) && id.equals(item.id()));
    }
}
