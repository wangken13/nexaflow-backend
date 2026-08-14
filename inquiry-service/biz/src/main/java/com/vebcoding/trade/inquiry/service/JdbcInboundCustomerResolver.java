package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.inquiry.api.InboundInquiryRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcInboundCustomerResolver implements InboundCustomerResolver {
    private final JdbcTemplate jdbcTemplate;

    JdbcInboundCustomerResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ResolvedCustomer resolve(String tenantId, InboundInquiryRequest request) {
        // Serializing only customer resolution per tenant avoids duplicate customer creation under concurrent webhooks.
        jdbcTemplate.queryForObject("SELECT id FROM tenants WHERE id=? FOR UPDATE", String.class, tenantId);
        String email = TextSanitizer.optional(request.email()).toLowerCase();
        if (!email.isBlank()) {
            List<String> matches = jdbcTemplate.queryForList("""
                    SELECT customer_id FROM customer_contacts
                    WHERE tenant_id=? AND LOWER(email)=? LIMIT 1
                    """, String.class, tenantId, email);
            if (!matches.isEmpty()) return new ResolvedCustomer(matches.getFirst(), false);
        }
        String customerName = TextSanitizer.required(request.customerName(), "客户名称");
        List<String> matches = jdbcTemplate.queryForList("""
                SELECT id FROM customers WHERE tenant_id=? AND LOWER(name)=LOWER(?) LIMIT 1
                """, String.class, tenantId, customerName);
        if (!matches.isEmpty()) return new ResolvedCustomer(matches.getFirst(), false);

        String customerId = "cus-" + UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO customers (id, tenant_id, name, country, tag) VALUES (?, ?, ?, ?, 'new')
                """, customerId, tenantId, customerName, blankToNull(request.country()));
        if (!email.isBlank() || !TextSanitizer.optional(request.phone()).isBlank()) {
            String contactName = TextSanitizer.optional(request.contactName());
            jdbcTemplate.update("""
                    INSERT INTO customer_contacts
                      (id, tenant_id, customer_id, name, email, phone, position, primary_flag)
                    VALUES (?, ?, ?, ?, ?, ?, '采购联系人', 1)
                    """, "con-" + UUID.randomUUID(), tenantId, customerId,
                    contactName.isBlank() ? customerName : contactName, blankToNull(email), blankToNull(request.phone()));
        }
        return new ResolvedCustomer(customerId, true);
    }

    private String blankToNull(String value) {
        String normalized = TextSanitizer.optional(value);
        return normalized.isBlank() ? null : normalized;
    }
}
