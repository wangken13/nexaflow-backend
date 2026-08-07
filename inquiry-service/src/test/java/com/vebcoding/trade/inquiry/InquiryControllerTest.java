package com.vebcoding.trade.inquiry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InquiryControllerTest {
    @Test
    void createMarksInquiryPendingAi() {
        InquiryRepository repository = new InMemoryInquiryRepository();
        InquiryEventPublisher publisher = inquiry -> {
        };
        InquiryService service = new InquiryService(repository, publisher);
        InquiryController controller = new InquiryController(service);

        var response = controller.create(new InquiryController.CreateInquiryRequest("cus-001", "Need quote", "500 pcs mug"));

        assertThat(response.data().status()).isEqualTo("PENDING_AI");
        assertThat(controller.list().data()).hasSize(1);
    }
}
