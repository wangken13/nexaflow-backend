package com.vebcoding.trade.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuotationControllerTest {
    @Test
    void createStartsAsDraft() {
        QuotationController controller = new QuotationController();
        var response = controller.create(new QuotationController.CreateQuotationRequest("cus-001", "Mug", 500, BigDecimal.ONE));
        assertThat(response.data().status()).isEqualTo("DRAFT");
    }
}
