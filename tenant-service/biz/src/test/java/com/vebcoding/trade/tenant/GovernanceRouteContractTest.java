package com.vebcoding.trade.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.tenant.api.CreateDepartmentRequest;
import com.vebcoding.trade.tenant.api.UpsertChannelConfigRequest;
import com.vebcoding.trade.tenant.api.UpsertKnowledgeArticleRequest;
import com.vebcoding.trade.tenant.controller.TenantController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class GovernanceRouteContractTest {
    @Test
    void exposesGovernanceRoutesWithExpectedHttpMethods() throws Exception {
        assertThat(TenantController.class.getAnnotation(RequestMapping.class).value()).containsExactly("/tenant");
        assertGet("departments", "/departments");
        assertPost("createDepartment", "/departments", CreateDepartmentRequest.class);
        assertGet("knowledgeArticles", "/knowledge");
        assertPost("createKnowledgeArticle", "/knowledge", UpsertKnowledgeArticleRequest.class);
        assertGet("channels", "/channels");
        assertPut("saveChannel", "/channels", UpsertChannelConfigRequest.class);
        assertGet("subscription", "/subscription");
        assertGet("importJobs", "/import-jobs");
    }

    private void assertGet(String methodName, String path) throws Exception {
        Method method = TenantController.class.getMethod(methodName);
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(path);
    }

    private void assertPost(String methodName, String path, Class<?> requestType) throws Exception {
        Method method = TenantController.class.getMethod(methodName, requestType);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(path);
    }

    private void assertPut(String methodName, String path, Class<?> requestType) throws Exception {
        Method method = TenantController.class.getMethod(methodName, requestType);
        assertThat(method.getAnnotation(PutMapping.class).value()).containsExactly(path);
    }
}
