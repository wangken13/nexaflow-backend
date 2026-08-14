package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.CustomerDetailView;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedSensitiveDataMasker implements SensitiveDataMasker {
    private static final Set<String> FULL_ACCESS_ROLES = Set.of("OWNER", "ADMIN", "SALES");

    public CustomerDetailView mask(CustomerDetailView detail, String role) {
        if (FULL_ACCESS_ROLES.contains(role.toUpperCase())) return detail;
        return new CustomerDetailView(detail.customer(), detail.contacts().stream().map(this::mask).toList(),
                detail.timeline());
    }

    private ContactView mask(ContactView contact) {
        return new ContactView(contact.id(), contact.customerId(), contact.name(), maskEmail(contact.email()),
                maskPhone(contact.phone()), contact.position(), contact.primary(), contact.createdAt());
    }

    private String maskEmail(String value) {
        if (value == null || value.isBlank()) return "";
        int at = value.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? value.substring(at) : "");
        return value.substring(0, 1) + "***" + value.substring(at);
    }

    private String maskPhone(String value) {
        if (value == null || value.isBlank()) return "";
        String compact = value.replaceAll("\\s+", "");
        if (compact.length() <= 7) return "***";
        return compact.substring(0, 3) + "****" + compact.substring(compact.length() - 4);
    }
}
