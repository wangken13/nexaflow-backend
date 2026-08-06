package com.vebcoding.trade.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class AiControllerTest {
    @Test
    void analyzeFallsBackWithoutModelKey() {
        var provider = new DefaultListableBeanFactory().getBeanProvider(ChatClient.Builder.class);
        AiController controller = new AiController(provider);
        var response = controller.analyzeInquiry(new AiController.AnalyzeInquiryRequest("urgent quote for 500 pcs"));
        assertThat(response.data().urgency()).isEqualTo("HIGH");
        assertThat(response.data().replyDraft()).contains("感谢");
    }
}
