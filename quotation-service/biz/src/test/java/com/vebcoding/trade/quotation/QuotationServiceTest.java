package com.vebcoding.trade.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.quotation.api.CreateQuotationRequest;
import com.vebcoding.trade.quotation.api.QuotationItemRequest;
import com.vebcoding.trade.quotation.mapper.InMemoryQuotationMapper;
import com.vebcoding.trade.quotation.service.QuotationService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class QuotationServiceTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("demo-tenant");
        TenantContext.setUserId("admin");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createQuotationStartsAsDraft() {
        QuotationService service = new QuotationService(new InMemoryQuotationMapper());

        var quotation = service.create(new CreateQuotationRequest("cus-001", "Ceramic mug", 500, BigDecimal.valueOf(1.25)));

        assertThat(quotation.status()).isEqualTo("DRAFT");
        assertThat(service.list()).hasSize(1);
    }

    @Test
    void createQuotationRejectsInvalidPrice() {
        QuotationService service = new QuotationService(new InMemoryQuotationMapper());

        assertThatThrownBy(() -> service.create(new CreateQuotationRequest("cus-001", "Mug", 1, BigDecimal.valueOf(-1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("单价不能小于0");
    }

    @Test
    void createProfessionalQuotationCalculatesItemsAndFreight() {
        QuotationService service = new QuotationService(new InMemoryQuotationMapper());
        var request = new CreateQuotationRequest("cus-001", "USD", "FOB", "Shanghai",
                BigDecimal.valueOf(80), "2026-08-30", "Lead time 30 days", List.of(
                new QuotationItemRequest("prd-1", "Generator", "1200W", 10, BigDecimal.valueOf(400)),
                new QuotationItemRequest("prd-2", "Cable", "US plug", 10, BigDecimal.valueOf(5))));

        var quotation = service.create(request);

        assertThat(quotation.items()).hasSize(2);
        assertThat(quotation.totalAmount()).isEqualByComparingTo("4130");
        assertThat(quotation.quotationNo()).startsWith("Q-");
    }

    @Test
    void quotationFollowsApprovalWorkflow() {
        QuotationService service = new QuotationService(new InMemoryQuotationMapper());
        var quotation = service.create(new CreateQuotationRequest("cus-001", "Mug", 100, BigDecimal.ONE));

        assertThat(service.updateStatus(quotation.id(), "PENDING_APPROVAL").status()).isEqualTo("PENDING_APPROVAL");
        assertThat(service.updateStatus(quotation.id(), "APPROVED").status()).isEqualTo("APPROVED");
        assertThat(service.updateStatus(quotation.id(), "SENT").status()).isEqualTo("SENT");
    }

    @Test
    void quotationRejectsSkippedApproval() {
        QuotationService service = new QuotationService(new InMemoryQuotationMapper());
        var quotation = service.create(new CreateQuotationRequest("cus-001", "Mug", 100, BigDecimal.ONE));

        assertThatThrownBy(() -> service.updateStatus(quotation.id(), "SENT"))
                .isInstanceOf(BusinessException.class);
    }
}
