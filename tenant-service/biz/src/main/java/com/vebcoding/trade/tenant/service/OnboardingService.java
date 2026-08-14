package com.vebcoding.trade.tenant.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.tenant.api.OnboardingStepView;
import com.vebcoding.trade.tenant.api.OnboardingView;
import com.vebcoding.trade.tenant.mapper.OnboardingStore;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {
    private final OnboardingStore store;

    public OnboardingService(OnboardingStore store) {
        this.store = store;
    }

    public OnboardingView status() {
        OnboardingStore.SetupCounts counts = store.counts(TenantContext.tenantId());
        List<OnboardingStepView> steps = List.of(
                step("MEMBER", "邀请团队成员", "建立协作账号并设置角色和数据范围", counts.additionalMembers()),
                step("CUSTOMER", "导入客户资料", "导入或创建第一份真实客户档案", counts.customers()),
                step("PRODUCT", "配置产品目录", "维护可直接用于报价的产品与价格", counts.products()),
                step("MAILBOX", "接入企业邮箱", "自动收取邮件并识别客户询盘", counts.activeMailboxes()),
                step("INQUIRY", "推进第一条询盘", "完成需求识别、跟进与下一步安排", counts.inquiries()));
        int completed = (int) steps.stream().filter(OnboardingStepView::completed).count();
        return new OnboardingView(completed, steps.size(), store.hasDemoData(TenantContext.tenantId()), steps);
    }

    @Transactional
    public OnboardingView createDemoData() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        String tenantId = TenantContext.tenantId();
        if (store.hasDemoData(tenantId)) throw BusinessException.conflict("演示数据已经存在，请先清理后再重新生成");
        String batchId = "demo-" + UUID.randomUUID();
        store.createDemoData(tenantId, TenantContext.userId(), new OnboardingStore.DemoIds(batchId,
                batchId + "-customer", batchId + "-product", batchId + "-inquiry", batchId + "-task"));
        return status();
    }

    @Transactional
    public OnboardingView clearDemoData() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!store.hasDemoData(TenantContext.tenantId())) throw BusinessException.notFound("当前企业没有可清理的演示数据");
        store.clearDemoData(TenantContext.tenantId());
        return status();
    }

    private OnboardingStepView step(String code, String title, String description, int count) {
        return new OnboardingStepView(code, title, description, count > 0, count);
    }
}
