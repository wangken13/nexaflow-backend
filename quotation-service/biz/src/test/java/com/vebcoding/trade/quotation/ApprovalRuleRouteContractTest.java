package com.vebcoding.trade.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.quotation.api.UpsertApprovalRuleRequest;
import com.vebcoding.trade.quotation.controller.QuotationController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class ApprovalRuleRouteContractTest {
    @Test
    void exposesApprovalRuleCollectionForReadingAndCreation() throws Exception {
        assertThat(QuotationController.class.getAnnotation(RequestMapping.class).value()).containsExactly("/quotation");

        Method list = QuotationController.class.getMethod("approvalRules");
        assertThat(list.getAnnotation(GetMapping.class).value()).containsExactly("/approval-rules");

        Method create = QuotationController.class.getMethod("createApprovalRule", UpsertApprovalRuleRequest.class);
        assertThat(create.getAnnotation(PostMapping.class).value()).containsExactly("/approval-rules");
    }
}
