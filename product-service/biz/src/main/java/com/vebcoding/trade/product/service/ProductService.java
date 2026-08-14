package com.vebcoding.trade.product.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.BulkImportResult;
import com.vebcoding.trade.common.ImportJobRecorder;
import com.vebcoding.trade.product.api.ProductView;
import com.vebcoding.trade.product.api.UpsertProductRequest;
import com.vebcoding.trade.product.mapper.ProductMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductMapper mapper;
    private final ImportJobRecorder importJobRecorder;
    public ProductService(ProductMapper mapper, ImportJobRecorder importJobRecorder) {
        this.mapper = mapper;
        this.importJobRecorder = importJobRecorder;
    }

    public List<ProductView> list(String keyword) {
        return mapper.findByTenantId(TenantContext.tenantId(), TextSanitizer.optional(keyword));
    }
    public List<ProductView> exportData() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return mapper.findByTenantId(TenantContext.tenantId(), "");
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

    public BulkImportResult bulkImport(List<UpsertProductRequest> rows) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        if (rows == null || rows.isEmpty() || rows.size() > 500) throw new BusinessException("单次导入数量必须为1至500条");
        String jobId = importJobRecorder.start("PRODUCT", rows.size());
        HashSet<String> skus = mapper.findByTenantId(TenantContext.tenantId(), "").stream()
                .map(item -> item.sku().trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<String> errors = new ArrayList<>();
        int imported = 0;
        for (int index = 0; index < rows.size(); index++) {
            UpsertProductRequest row = rows.get(index);
            String sku = TextSanitizer.optional(row.sku());
            if (sku.isBlank()) { errors.add("第" + (index + 1) + "行：SKU不能为空"); continue; }
            if (!skus.add(sku.toLowerCase(Locale.ROOT))) { errors.add("第" + (index + 1) + "行：SKU重复"); continue; }
            try { create(row); imported++; }
            catch (BusinessException ex) { errors.add("第" + (index + 1) + "行：" + ex.getMessage()); }
        }
        return importJobRecorder.complete(jobId, rows.size(), imported, errors);
    }
}
