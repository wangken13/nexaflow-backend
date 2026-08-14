package com.vebcoding.trade.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vebcoding.trade.tenant.mapper.JdbcOnboardingStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcOnboardingStoreTest {
    @Test
    void onboardingCountsUseTheMigratedMailboxTable() {
        JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        String tenantId = "tenant-test";
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Integer.class), org.mockito.ArgumentMatchers.eq(tenantId)))
                .thenReturn(0);

        var counts = new JdbcOnboardingStore(jdbc).counts(tenantId);

        assertThat(counts.activeMailboxes()).isZero();
        verify(jdbc).queryForObject("SELECT COUNT(*) FROM email_mailbox_configs WHERE tenant_id=? AND active_flag=1",
                Integer.class, tenantId);
    }
}
