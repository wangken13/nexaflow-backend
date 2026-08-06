package com.vebcoding.trade.inquiry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class InquiryControllerTest {
    @Test
    void createMarksInquiryPendingAi() {
        var provider = new DefaultListableBeanFactory().getBeanProvider(RabbitTemplate.class);
        InquiryController controller = new InquiryController(provider);
        var response = controller.create(new InquiryController.CreateInquiryRequest("cus-001", "Need quote", "500 pcs mug"));
        assertThat(response.data().status()).isEqualTo("PENDING_AI");
    }
}
