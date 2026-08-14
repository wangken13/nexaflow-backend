package com.vebcoding.trade.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.product.api.UpsertProductRequest;
import com.vebcoding.trade.product.mapper.InMemoryProductMapper;
import com.vebcoding.trade.product.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class ProductServiceTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("admin");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void productLifecycleIsTenantScoped() {
        ProductService service = new ProductService(new InMemoryProductMapper());
        var created = service.create(new UpsertProductRequest("MUG-01", "Ceramic mug", "350ml", "usd",
                BigDecimal.valueOf(1.25), 500, true));
        assertThat(service.get(created.id()).currency()).isEqualTo("USD");
        assertThat(service.list("MUG")).hasSize(1);
        service.delete(created.id());
        assertThat(service.list("")).isEmpty();
    }

    @Test
    void bulkImportValidatesSkuAndBusinessFields() {
        ProductService service = new ProductService(new InMemoryProductMapper());
        var result = service.bulkImport(List.of(
                new UpsertProductRequest("SKU-1", "Mug", "350ml", "USD", BigDecimal.ONE, 100, true),
                new UpsertProductRequest("SKU-1", "Duplicate", "", "USD", BigDecimal.ONE, 100, true),
                new UpsertProductRequest("SKU-2", "Invalid", "", "USD", BigDecimal.valueOf(-1), 100, true)));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
    }
}
