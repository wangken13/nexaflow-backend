package com.vebcoding.trade.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.AccessDeniedException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.tenant.api.CreateMemberRequest;
import com.vebcoding.trade.tenant.mapper.InMemoryTenantMapper;
import com.vebcoding.trade.tenant.service.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TenantServiceTest {
    private final TenantService service = new TenantService(new InMemoryTenantMapper(), new BCryptPasswordEncoder(4));

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void ownerCanCreateMemberAndAuditOperation() {
        TenantContext.setTenantId("tenant-demo");
        TenantContext.setRole("OWNER");
        TenantContext.setUserId("admin");

        var member = service.createMember(new CreateMemberRequest("sales01", "securePass123", "销售一组", "sales@example.com", "SALES"));

        assertThat(member.status()).isEqualTo("ACTIVE");
        assertThat(service.members()).extracting("username").contains("sales01");
        assertThat(service.auditLogs("", "")).extracting("action").contains("MEMBER_CREATED");
    }

    @Test
    void salesCannotReadMemberDirectory() {
        TenantContext.setTenantId("tenant-demo");
        TenantContext.setUserId("sales01");
        TenantContext.setRole("SALES");

        assertThatThrownBy(service::members).isInstanceOf(AccessDeniedException.class);
    }
}
