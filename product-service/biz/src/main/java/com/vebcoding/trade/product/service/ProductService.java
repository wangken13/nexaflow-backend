package com.vebcoding.trade.product.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.product.api.ProductView;
import com.vebcoding.trade.product.api.UpsertProductRequest;
import com.vebcoding.trade.product.mapper.ProductMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductMapper mapper;
    public ProductService(ProductMapper mapper) { this.mapper = mapper; }

    public List<ProductView> list(String keyword) {
        return mapper.findByTenantId(TenantContext.tenantId(), TextSanitizer.optional(keyword));
    }
    public ProductView get(String id) {
        return mapper.findByTenantIdAndId(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("产品不存在"));
    }
    public ProductView create(UpsertProductRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        return mapper.save(toView("prd-" + UUID.randomUUID(), Instant.now().toString(), request));
    }
    public ProductView update(String id, UpsertProductRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        ProductView current = get(id);
        return mapper.save(toView(current.id(), current.createdAt(), request));
    }
    public void delete(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!mapper.delete(TenantContext.tenantId(), id)) throw BusinessException.notFound("产品不存在");
    }
    private ProductView toView(String id, String createdAt, UpsertProductRequest request) {
        String sku = TextSanitizer.required(request.sku(), "产品SKU");
        String name = TextSanitizer.required(request.name(), "产品名称");
        String currency = TextSanitizer.optional(request.currency()).toUpperCase();
        if (currency.isBlank()) currency = "USD";
        BigDecimal price = request.unitPrice() == null ? BigDecimal.ZERO : request.unitPrice();
        if (price.signum() < 0) throw new BusinessException("产品价格不能小于0");
        if (request.moq() <= 0) throw new BusinessException("最小起订量必须大于0");
        return new ProductView(id, TenantContext.tenantId(), sku, name, TextSanitizer.optional(request.specification()),
                currency, price, request.moq(), request.active(), createdAt);
    }
}
