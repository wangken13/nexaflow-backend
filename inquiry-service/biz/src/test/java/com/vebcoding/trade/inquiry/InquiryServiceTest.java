package com.vebcoding.trade.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.inquiry.api.CreateInquiryRequest;
import com.vebcoding.trade.inquiry.api.InquiryView;
import com.vebcoding.trade.inquiry.mapper.InMemoryInquiryMapper;
import com.vebcoding.trade.inquiry.service.InquiryService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class InquiryServiceTest {
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
    void createPublishesCreatedEventAndAllowsStatusTransition() {
        AtomicReference<InquiryView> published = new AtomicReference<>();
        InquiryService service = new InquiryService(new InMemoryInquiryMapper(), published::set);

        InquiryView inquiry = service.create(new CreateInquiryRequest(" cus-001 ", " Need quote ", " 500 pcs "));
        var updated = service.updateStatus(inquiry.id(), "ANALYZED");

        assertThat(published.get().id()).isEqualTo(inquiry.id());
        assertThat(updated).isPresent();
        assertThat(updated.orElseThrow().status()).isEqualTo("ANALYZED");
    }

    @Test
    void updateStatusRejectsUnsupportedStatus() {
        InquiryService service = new InquiryService(new InMemoryInquiryMapper(), inquiry -> {
        });

        assertThatThrownBy(() -> service.updateStatus("inq-001", "BROKEN"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("询盘状态不合法");
    }
}
