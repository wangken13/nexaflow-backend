package com.vebcoding.trade.inquiry;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.inquiry.api.CreateInquiryRequest;
import com.vebcoding.trade.inquiry.controller.InquiryController;
import com.vebcoding.trade.inquiry.mapper.InMemoryInquiryMapper;
import com.vebcoding.trade.inquiry.mapper.InquiryMapper;
import com.vebcoding.trade.inquiry.service.InquiryEventPublisher;
import com.vebcoding.trade.inquiry.service.InquiryService;
import org.junit.jupiter.api.Test;

class InquiryControllerTest {
    @Test
    void createMarksInquiryPendingAi() {
        InquiryMapper mapper = new InMemoryInquiryMapper();
        InquiryEventPublisher publisher = inquiry -> {
        };
        InquiryService service = new InquiryService(mapper, publisher);
        InquiryController controller = new InquiryController(service);

        var response = controller.create(new CreateInquiryRequest("cus-001", "Need quote", "500 pcs mug"));

        assertThat(response.data().status()).isEqualTo("PENDING_AI");
        assertThat(controller.list().data()).hasSize(1);
    }
}
