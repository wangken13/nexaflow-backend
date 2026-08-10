package com.vebcoding.trade.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vebcoding.trade.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SpugSmsSenderTest {
    private static final String LOGIN_URL = "https://push.spug.cc/send/login-template";
    private static final String REGISTER_URL = "https://push.spug.cc/send/register-template";

    @Test
    void sendsLoginCodeUsingSpugPayloadAndDomesticPhone() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(LOGIN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json("""
                        {"name":"NexaFlow","code":"153146","targets":"13800000000"}
                        """))
                .andRespond(withSuccess());

        new SpugSmsSender(builder, LOGIN_URL, REGISTER_URL, "NexaFlow")
                .send("+8613800000000", "153146", "LOGIN");

        server.verify();
    }

    @Test
    void usesRegisterTemplateAndKeepsInternationalPhone() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(REGISTER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"name":"NexaFlow","code":"654321","targets":"+14155552671"}
                        """))
                .andRespond(withSuccess());

        new SpugSmsSender(builder, LOGIN_URL, REGISTER_URL, "NexaFlow")
                .send("+14155552671", "654321", "REGISTER");

        server.verify();
    }

    @Test
    void rejectsMissingTemplateConfiguration() {
        SpugSmsSender sender = new SpugSmsSender(
                RestClient.builder(), "", REGISTER_URL, "NexaFlow");

        assertThatThrownBy(() -> sender.send("+8613800000000", "123456", "LOGIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录短信模板地址尚未配置");
    }

    @Test
    void rejectsUnsafeTemplateUrl() {
        SpugSmsSender sender = new SpugSmsSender(
                RestClient.builder(), "http://push.spug.cc/send/login-template", REGISTER_URL, "NexaFlow");

        assertThatThrownBy(() -> sender.send("+8613800000000", "123456", "LOGIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板地址格式错误");
    }

    @Test
    void rejectsUnknownPurpose() {
        SpugSmsSender sender = new SpugSmsSender(
                RestClient.builder(), LOGIN_URL, REGISTER_URL, "NexaFlow");

        assertThatThrownBy(() -> sender.send("+8613800000000", "123456", "RESET_PASSWORD"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码用途不合法");
    }

    @Test
    void acceptsSmsEndpointGeneratedBySpugConsole() {
        String smsUrl = "https://push.spug.cc/sms/sms-credential-example";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(smsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"code":"123456","number":"5","to":"13800000000"}
                        """))
                .andRespond(withSuccess());

        new SpugSmsSender(builder, smsUrl, smsUrl, "NexaFlow")
                .send("+8613800000000", "123456", "REGISTER");

        server.verify();
    }

    @Test
    void rejectsBusinessFailureReturnedWithHttpSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(LOGIN_URL))
                .andRespond(withSuccess("""
                        {"error":1,"message":"账户余额不足"}
                        """, MediaType.APPLICATION_JSON));
        SpugSmsSender sender = new SpugSmsSender(
                builder, LOGIN_URL, REGISTER_URL, "NexaFlow");

        assertThatThrownBy(() -> sender.send("+8613800000000", "123456", "LOGIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("短信平台发送失败：账户余额不足");

        server.verify();
    }

    @Test
    void acceptsSpugSuccessResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(LOGIN_URL))
                .andRespond(withSuccess("""
                        {"error":0,"message":"success"}
                        """, MediaType.APPLICATION_JSON));

        new SpugSmsSender(builder, LOGIN_URL, REGISTER_URL, "NexaFlow")
                .send("+8613800000000", "123456", "LOGIN");

        server.verify();
    }
}
