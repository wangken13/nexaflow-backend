package com.vebcoding.trade.customer;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    private final List<CustomerView> customers = new CopyOnWriteArrayList<>(List.of(
            new CustomerView("cus-001", "demo-tenant", "North Star Imports", "USA", "vip", Instant.now().toString()),
            new CustomerView("cus-002", "demo-tenant", "Atlas Homeware", "Germany", "new", Instant.now().toString())));

    @GetMapping
    public ApiResponse<List<CustomerView>> list() {
        String tenantId = TenantContext.tenantId();
        return ApiResponse.ok(customers.stream().filter(item -> tenantId.equals(item.tenantId())).toList());
    }

    @PostMapping
    public ApiResponse<CustomerView> create(@RequestBody CreateCustomerRequest request) {
        CustomerView customer = new CustomerView("cus-" + UUID.randomUUID(), TenantContext.tenantId(),
                request.name(), request.country(), request.tag(), Instant.now().toString());
        customers.add(customer);
        return ApiResponse.ok(customer);
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(new ArrayList<>(List.of("new", "vip", "at-risk", "quoted")));
    }

    public record CreateCustomerRequest(@NotBlank String name, String country, String tag) {
    }

    public record CustomerView(String id, String tenantId, String name, String country, String tag, String createdAt) {
    }
}
