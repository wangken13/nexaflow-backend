package com.vebcoding.trade.quotation.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.quotation.api.CreateQuotationRequest;
import com.vebcoding.trade.quotation.api.QuotationItemRequest;
import com.vebcoding.trade.quotation.api.QuotationItemView;
import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.api.QuotationApprovalView;
import com.vebcoding.trade.quotation.api.ApprovalRuleView;
import com.vebcoding.trade.quotation.api.UpsertApprovalRuleRequest;
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
import java.util.Locale;
import com.vebcoding.trade.common.RoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationService {
    private static final Set<String> RULE_TYPES = Set.of("AMOUNT_THRESHOLD", "VIP_CUSTOMER", "TRADE_TERM");
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
    public List<QuotationApprovalView> approvals(String id) { get(id); return quotationMapper.findApprovals(TenantContext.tenantId(), id); }

    public List<ApprovalRuleView> approvalRules() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return quotationMapper.findApprovalRules(TenantContext.tenantId());
    }

    @Transactional
    public ApprovalRuleView createApprovalRule(UpsertApprovalRuleRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        Instant now = Instant.now();
        return quotationMapper.saveApprovalRule(toApprovalRule("rule-" + UUID.randomUUID(), request,
                now.toString(), now.toString()));
    }

    @Transactional
    public ApprovalRuleView updateApprovalRule(String id, UpsertApprovalRuleRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        ApprovalRuleView current = quotationMapper.findApprovalRule(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("审批规则不存在"));
        return quotationMapper.saveApprovalRule(toApprovalRule(current.id(), request, current.createdAt(),
                Instant.now().toString()));
    }

    @Transactional
    public void deleteApprovalRule(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!quotationMapper.deleteApprovalRule(TenantContext.tenantId(), id)) {
            throw BusinessException.notFound("审批规则不存在");
        }
    }

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
        ApprovalDecision decision = evaluateApproval(customerId, total, tradeTerm);
        QuotationView saved = quotationMapper.save(new QuotationView(id, TenantContext.tenantId(), customerId, number,
                first.productName(), first.quantity(), first.unitPrice(), currency, tradeTerm,
                TextSanitizer.optional(request.destinationPort()), freight, total, validUntil,
                TextSanitizer.optional(request.notes()), decision.required(), decision.reason(),
                decision.required() ? "PENDING_APPROVAL" : "DRAFT", items, Instant.now().toString()));
        if (decision.required()) {
            saveApproval(id, "AUTO_SUBMITTED", decision.reason());
        }
        return saved;
    }

    @Transactional
    public QuotationView updateStatus(String id, String status) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        String normalized = normalize(status, "");
        QuotationView current = get(id);
        Set<String> allowed = TRANSITIONS.getOrDefault(current.status(), Set.of());
        if (current.status().equals("DRAFT") && !current.approvalRequired()) {
            allowed = Set.of("PENDING_APPROVAL", "SENT");
        }
        if (!allowed.contains(normalized)) {
            throw BusinessException.conflict("报价单不能从 " + current.status() + " 变更为 " + normalized);
        }
        if (normalized.equals("APPROVED")) {
            RoleGuard.requireAny("OWNER", "ADMIN");
        }
        return quotationMapper.save(new QuotationView(current.id(), current.tenantId(), current.customerId(),
                current.quotationNo(), current.productName(), current.quantity(), current.unitPrice(),
                current.currency(), current.tradeTerm(), current.destinationPort(), current.freight(),
                current.totalAmount(), current.validUntil(), current.notes(), current.approvalRequired(),
                current.approvalReason(), normalized, current.items(), current.createdAt()));
    }

    @Transactional
    public QuotationView submitApproval(String id, String comment) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES");
        QuotationView updated = updateStatus(id, "PENDING_APPROVAL");
        saveApproval(id, "SUBMITTED", comment);
        return updated;
    }

    @Transactional
    public QuotationView decideApproval(String id, boolean approved, String comment) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!approved && TextSanitizer.optional(comment).isBlank()) {
            throw new BusinessException("驳回报价时必须填写原因");
        }
        QuotationView updated = updateStatus(id, approved ? "APPROVED" : "REJECTED");
        saveApproval(id, approved ? "APPROVED" : "REJECTED", comment);
        return updated;
    }

    private void saveApproval(String quotationId, String action, String comment) {
        quotationMapper.saveApproval(TenantContext.tenantId(), new QuotationApprovalView(
                "apr-" + UUID.randomUUID(), quotationId, action, TextSanitizer.optional(comment),
                TenantContext.userId(), Instant.now().toString()));
    }

    private ApprovalRuleView toApprovalRule(String id, UpsertApprovalRuleRequest request,
                                            String createdAt, String updatedAt) {
        String ruleType = normalize(request.ruleType(), "");
        if (!RULE_TYPES.contains(ruleType)) throw new BusinessException("审批规则类型不合法");
        BigDecimal threshold = request.thresholdAmount();
        String conditionValue = TextSanitizer.optional(request.conditionValue());
        if (ruleType.equals("AMOUNT_THRESHOLD") && (threshold == null || threshold.signum() <= 0)) {
            throw new BusinessException("金额审批阈值必须大于0");
        }
        if (!ruleType.equals("AMOUNT_THRESHOLD") && conditionValue.isBlank()) {
            throw new BusinessException("请填写审批规则条件");
        }
        return new ApprovalRuleView(id, TenantContext.tenantId(),
                TextSanitizer.required(request.name(), "规则名称"), ruleType,
                ruleType.equals("AMOUNT_THRESHOLD") ? threshold : null,
                ruleType.equals("AMOUNT_THRESHOLD") ? "" : conditionValue.toUpperCase(Locale.ROOT),
                request.enabled(), createdAt, updatedAt);
    }

    private ApprovalDecision evaluateApproval(String customerId, BigDecimal totalAmount, String tradeTerm) {
        String customerTag = quotationMapper.findCustomerTag(TenantContext.tenantId(), customerId);
        List<String> reasons = quotationMapper.findApprovalRules(TenantContext.tenantId()).stream()
                .filter(ApprovalRuleView::enabled)
                .filter(rule -> matches(rule, totalAmount, customerTag, tradeTerm))
                .map(ApprovalRuleView::name)
                .toList();
        return reasons.isEmpty() ? new ApprovalDecision(false, "")
                : new ApprovalDecision(true, "命中审批规则：" + String.join("、", reasons));
    }

    private boolean matches(ApprovalRuleView rule, BigDecimal totalAmount, String customerTag, String tradeTerm) {
        return switch (rule.ruleType()) {
            case "AMOUNT_THRESHOLD" -> rule.thresholdAmount() != null
                    && totalAmount.compareTo(rule.thresholdAmount()) >= 0;
            case "VIP_CUSTOMER" -> rule.conditionValue().equalsIgnoreCase(customerTag);
            case "TRADE_TERM" -> rule.conditionValue().equalsIgnoreCase(tradeTerm);
            default -> false;
        };
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

    private record ApprovalDecision(boolean required, String reason) {
    }
}
