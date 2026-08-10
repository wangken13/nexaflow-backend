package com.vebcoding.trade.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.product.api.UpsertProductRequest;
import com.vebcoding.trade.product.mapper.InMemoryProductMapper;
import com.vebcoding.trade.product.service.ProductService;
import java.math.BigDecimal;
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
}
