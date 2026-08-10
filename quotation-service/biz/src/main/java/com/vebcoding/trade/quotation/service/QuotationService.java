package com.vebcoding.trade.quotation.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.quotation.api.CreateQuotationRequest;
import com.vebcoding.trade.quotation.api.QuotationItemRequest;
import com.vebcoding.trade.quotation.api.QuotationItemView;
import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.mapper.QuotationMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.vebcoding.trade.common.RoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationService {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "DRAFT", Set.of("PENDING_APPROVAL"),
            "PENDING_APPROVAL", Set.of("DRAFT", "APPROVED", "REJECTED"),
            "APPROVED", Set.of("SENT"),
            "SENT", Set.of("ACCEPTED", "REJECTED", "EXPIRED"),
            "ACCEPTED", Set.of(),
            "REJECTED", Set.of("DRAFT"),
            "EXPIRED", Set.of("DRAFT"));
    private final QuotationMapper quotationMapper;
    public QuotationService(QuotationMapper quotationMapper) { this.quotationMapper = quotationMapper; }

    public List<QuotationView> list() { return quotationMapper.findByTenantId(TenantContext.tenantId()); }
    public QuotationView get(String id) { return quotationMapper.findByTenantIdAndId(TenantContext.tenantId(), id)
            .orElseThrow(() -> BusinessException.notFound("报价单不存在")); }

    @Transactional
    public QuotationView create(CreateQuotationRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String customerId = TextSanitizer.required(request.customerId(), "客户ID");
        if (request.items() == null || request.items().isEmpty()) throw new BusinessException("报价单至少需要一个明细");
        List<QuotationItemView> items = request.items().stream().map(this::toItem).toList();
        BigDecimal freight = request.freight() == null ? BigDecimal.ZERO : request.freight();
        if (freight.signum() < 0) throw new BusinessException("运费不能小于0");
        String currency = normalize(request.currency(), "USD");
        String tradeTerm = normalize(request.tradeTerm(), "FOB");
        String validUntil = request.validUntil() == null || request.validUntil().isBlank()
                ? LocalDate.now().plusDays(14).toString() : request.validUntil();
        String id = "quo-" + UUID.randomUUID();
        String number = "Q-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + id.substring(id.length() - 6).toUpperCase();
        BigDecimal total = items.stream().map(QuotationItemView::amount).reduce(BigDecimal.ZERO, BigDecimal::add).add(freight);
        QuotationItemView first = items.getFirst();
        return quotationMapper.save(new QuotationView(id, TenantContext.tenantId(), customerId, number,
                first.productName(), first.quantity(), first.unitPrice(), currency, tradeTerm,
                TextSanitizer.optional(request.destinationPort()), freight, total, validUntil,
                TextSanitizer.optional(request.notes()), "DRAFT", items, Instant.now().toString()));
    }

    @Transactional
    public QuotationView updateStatus(String id, String status) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String normalized = normalize(status, "");
        QuotationView current = get(id);
        if (!TRANSITIONS.getOrDefault(current.status(), Set.of()).contains(normalized)) {
            throw BusinessException.conflict("报价单不能从 " + current.status() + " 变更为 " + normalized);
        }
        if (normalized.equals("APPROVED")) {
            RoleGuard.requireAny("OWNER", "ADMIN");
        }
        return quotationMapper.save(new QuotationView(current.id(), current.tenantId(), current.customerId(),
                current.quotationNo(), current.productName(), current.quantity(), current.unitPrice(),
                current.currency(), current.tradeTerm(), current.destinationPort(), current.freight(),
                current.totalAmount(), current.validUntil(), current.notes(), normalized, current.items(), current.createdAt()));
    }

    private QuotationItemView toItem(QuotationItemRequest request) {
        String name = TextSanitizer.required(request.productName(), "产品名称");
        if (request.quantity() <= 0) throw new BusinessException("报价数量必须大于0");
        if (request.unitPrice() == null || request.unitPrice().signum() < 0) throw new BusinessException("单价不能小于0");
        BigDecimal amount = request.unitPrice().multiply(BigDecimal.valueOf(request.quantity()));
        return new QuotationItemView("qit-" + UUID.randomUUID(), TextSanitizer.optional(request.productId()), name,
                TextSanitizer.optional(request.specification()), request.quantity(), request.unitPrice(), amount);
    }
    private String normalize(String value, String fallback) {
        String normalized = TextSanitizer.optional(value).toUpperCase(); return normalized.isBlank() ? fallback : normalized;
    }
}
