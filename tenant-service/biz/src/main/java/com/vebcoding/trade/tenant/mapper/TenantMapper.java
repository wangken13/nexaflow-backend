package com.vebcoding.trade.tenant.mapper;

import com.vebcoding.trade.tenant.api.TenantProfileResponse;
import com.vebcoding.trade.tenant.api.AuditLogView;
import com.vebcoding.trade.tenant.api.MemberView;
import com.vebcoding.trade.tenant.api.ChannelConfigView;
import com.vebcoding.trade.tenant.api.KnowledgeArticleView;
import com.vebcoding.trade.tenant.api.SubscriptionView;
import java.util.List;
import java.util.Optional;

public interface TenantMapper {
    TenantProfileResponse findProfile(String tenantId);

    List<MemberView> findMembers(String tenantId);

    Optional<MemberView> findMember(String tenantId, String id);

    MemberView saveMember(MemberView member, String passwordHash);

    MemberView updateMemberRole(String tenantId, String id, String role);

    MemberView updateMemberStatus(String tenantId, String id, String status);

    List<AuditLogView> findAuditLogs(String tenantId, String module, String keyword);

    AuditLogView saveAuditLog(AuditLogView auditLog);

    List<KnowledgeArticleView> findKnowledgeArticles(String tenantId);

    Optional<KnowledgeArticleView> findKnowledgeArticle(String tenantId, String id);

    KnowledgeArticleView saveKnowledgeArticle(KnowledgeArticleView article);

    boolean deleteKnowledgeArticle(String tenantId, String id);

    List<ChannelConfigView> findChannelConfigs(String tenantId);

    ChannelConfigView saveChannelConfig(String tenantId, ChannelConfigView channel, String updatedBy);

    SubscriptionView findSubscription(String tenantId);
}
