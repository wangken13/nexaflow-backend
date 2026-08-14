package com.vebcoding.trade.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.AccessDeniedException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.tenant.mapper.OnboardingStore;
import com.vebcoding.trade.tenant.service.OnboardingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OnboardingServiceTest {
    private final FakeStore store = new FakeStore();
    private final OnboardingService service = new OnboardingService(store);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    @Test
    void reportsRealSetupProgress() {
        context("OWNER");
        store.counts = new OnboardingStore.SetupCounts(1, 2, 3, 0, 1);

        var status = service.status();

        assertThat(status.completedSteps()).isEqualTo(4);
        assertThat(status.steps()).filteredOn(step -> step.code().equals("MAILBOX"))
                .extracting("completed").containsExactly(false);
    }

    @Test
    void ownerCanCreateAndClearOnlyTrackedDemoData() {
        context("OWNER");

        assertThat(service.createDemoData().demoDataPresent()).isTrue();
        assertThat(store.created).isTrue();
        assertThat(service.clearDemoData().demoDataPresent()).isFalse();
        assertThat(store.cleared).isTrue();
    }

    @Test
    void salesCannotManageDemoData() {
        context("SALES");
        assertThatThrownBy(service::createDemoData).isInstanceOf(AccessDeniedException.class);
    }

    private void context(String role) {
        TenantContext.setTenantId("tenant-demo");
        TenantContext.setUserId("user-demo");
        TenantContext.setRole(role);
    }

    private static final class FakeStore implements OnboardingStore {
        private SetupCounts counts = new SetupCounts(0, 0, 0, 0, 0);
        private boolean demo;
        private boolean created;
        private boolean cleared;

        @Override public SetupCounts counts(String tenantId) { return counts; }
        @Override public boolean hasDemoData(String tenantId) { return demo; }
        @Override public void createDemoData(String tenantId, String userId, DemoIds ids) {
            created = true; demo = true;
        }
        @Override public int clearDemoData(String tenantId) { cleared = true; demo = false; return 4; }
    }
}
