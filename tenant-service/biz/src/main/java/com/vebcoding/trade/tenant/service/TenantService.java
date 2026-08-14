package com.vebcoding.trade.tenant.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.CreateMemberRequest;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.ChannelConfigView;
import com.vebcoding.trade.tenant.api.KnowledgeArticleView;
import com.vebcoding.trade.tenant.api.SubscriptionView;
import com.vebcoding.trade.tenant.api.UpsertChannelConfigRequest;
import com.vebcoding.trade.tenant.api.UpsertKnowledgeArticleRequest;
import com.vebcoding.trade.tenant.mapper.TenantMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {
    private static final Set<String> ROLES = Set.of("OWNER", "ADMIN", "SALES", "OPERATOR", "VIEWER");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> KNOWLEDGE_CATEGORIES = Set.of("PRODUCT", "PRICING", "DELIVERY", "POLICY", "FAQ");
    private static final Set<String> CHANNEL_TYPES = Set.of("EMAIL", "WEBSITE", "WHATSAPP", "WECHAT_WORK");
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantMapper tenantMapper, PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public TenantProfileResponse profile() {
        return tenantMapper.findProfile(TenantContext.tenantId());
    }

    public List<MemberView> members() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return tenantMapper.findMembers(TenantContext.tenantId());
    }

    @Transactional
    public MemberView createMember(CreateMemberRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        String role = normalizeRole(request.role());
        if (role.equals("OWNER") && !TenantContext.role().equals("OWNER")) {
            throw new BusinessException("只有企业所有者可以新增所有者");
        }
        String id = "user-" + UUID.randomUUID();
        MemberView member = new MemberView(id, TenantContext.tenantId(),
                TextSanitizer.required(request.username(), "登录账号"),
                TextSanitizer.required(request.displayName(), "成员姓名"), TextSanitizer.optional(request.email()),
                role, "ACTIVE", Instant.now().toString());
        try {
            MemberView saved = tenantMapper.saveMember(member, passwordEncoder.encode(request.password()));
            audit("TENANT", "MEMBER_CREATED", saved.id(), "新增成员 " + saved.username());
            return saved;
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw BusinessException.conflict("登录账号已存在");
        }
    }

    @Transactional
    public MemberView updateMemberRole(String id, String role) {
        RoleGuard.requireAny("OWNER");
        MemberView current = member(id);
        if (current.username().equals(TenantContext.userId())) {
            throw BusinessException.conflict("不能修改自己的角色");
        }
        MemberView updated = tenantMapper.updateMemberRole(TenantContext.tenantId(), id, normalizeRole(role));
        audit("TENANT", "MEMBER_ROLE_CHANGED", id, current.role() + " -> " + updated.role());
        return updated;
    }

    @Transactional
    public MemberView updateMemberStatus(String id, String status) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        MemberView current = member(id);
        if (current.username().equals(TenantContext.userId())) {
            throw BusinessException.conflict("不能停用当前登录账号");
        }
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) throw new BusinessException("成员状态不合法");
        MemberView updated = tenantMapper.updateMemberStatus(TenantContext.tenantId(), id, normalized);
        audit("TENANT", "MEMBER_STATUS_CHANGED", id, current.status() + " -> " + updated.status());
        return updated;
    }

    public List<AuditLogView> auditLogs(String module, String keyword) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return tenantMapper.findAuditLogs(TenantContext.tenantId(), module, keyword);
    }

    public List<KnowledgeArticleView> knowledgeArticles() {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES", "OPERATOR");
        return tenantMapper.findKnowledgeArticles(TenantContext.tenantId());
    }

    @Transactional
    public KnowledgeArticleView createKnowledgeArticle(UpsertKnowledgeArticleRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        Instant now = Instant.now();
        KnowledgeArticleView article = new KnowledgeArticleView("kb-" + UUID.randomUUID(), TenantContext.tenantId(),
                TextSanitizer.required(request.title(), "知识标题"), normalizeKnowledgeCategory(request.category()),
                TextSanitizer.required(request.content(), "知识内容"), request.active(), TenantContext.userId(),
                now.toString(), now.toString());
        KnowledgeArticleView saved = tenantMapper.saveKnowledgeArticle(article);
        audit("KNOWLEDGE", "ARTICLE_CREATED", saved.id(), "新增知识条目 " + saved.title());
        return saved;
    }

    @Transactional
    public KnowledgeArticleView updateKnowledgeArticle(String id, UpsertKnowledgeArticleRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN", "OPERATOR");
        KnowledgeArticleView current = tenantMapper.findKnowledgeArticle(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("知识条目不存在"));
        KnowledgeArticleView updated = new KnowledgeArticleView(current.id(), current.tenantId(),
                TextSanitizer.required(request.title(), "知识标题"), normalizeKnowledgeCategory(request.category()),
                TextSanitizer.required(request.content(), "知识内容"), request.active(), TenantContext.userId(),
                current.createdAt(), Instant.now().toString());
        tenantMapper.saveKnowledgeArticle(updated);
        audit("KNOWLEDGE", "ARTICLE_UPDATED", id, "更新知识条目 " + updated.title());
        return updated;
    }

    @Transactional
    public void deleteKnowledgeArticle(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!tenantMapper.deleteKnowledgeArticle(TenantContext.tenantId(), id)) {
            throw BusinessException.notFound("知识条目不存在");
        }
        audit("KNOWLEDGE", "ARTICLE_DELETED", id, "删除知识条目");
    }

    public List<ChannelConfigView> channels() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return tenantMapper.findChannelConfigs(TenantContext.tenantId());
    }

    @Transactional
    public ChannelConfigView saveChannel(UpsertChannelConfigRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        String type = normalizeChannelType(request.channelType());
        String accountRef = TextSanitizer.optional(request.accountRef());
        String status = request.enabled() && !accountRef.isBlank() ? "CONFIGURED" : "UNVERIFIED";
        ChannelConfigView channel = new ChannelConfigView("chn-" + UUID.randomUUID(), type,
                TextSanitizer.required(request.displayName(), "渠道名称"), accountRef,
                request.enabled(), status, Instant.now().toString());
        ChannelConfigView saved = tenantMapper.saveChannelConfig(TenantContext.tenantId(), channel, TenantContext.userId());
        audit("CHANNEL", "CHANNEL_CONFIGURED", saved.id(), "配置渠道 " + saved.displayName());
        return saved;
    }

    public SubscriptionView subscription() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return tenantMapper.findSubscription(TenantContext.tenantId());
    }

    private MemberView member(String id) {
        return tenantMapper.findMember(TenantContext.tenantId(), id)
                .orElseThrow(() -> BusinessException.notFound("成员不存在"));
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) throw new BusinessException("成员角色不合法");
        return normalized;
    }

    private String normalizeKnowledgeCategory(String category) {
        String normalized = TextSanitizer.optional(category).toUpperCase(Locale.ROOT);
        if (!KNOWLEDGE_CATEGORIES.contains(normalized)) throw new BusinessException("知识分类不合法");
        return normalized;
    }

    private String normalizeChannelType(String channelType) {
        String normalized = TextSanitizer.optional(channelType).toUpperCase(Locale.ROOT);
        if (!CHANNEL_TYPES.contains(normalized)) throw new BusinessException("渠道类型不合法");
        return normalized;
    }

    private void audit(String module, String action, String targetId, String detail) {
        tenantMapper.saveAuditLog(new AuditLogView("audit-" + UUID.randomUUID(), TenantContext.tenantId(),
                TenantContext.userId(), module, action, targetId, detail, Instant.now().toString()));
    }
}
