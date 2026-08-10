package com.vebcoding.trade.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vebcoding.trade.common.BusinessException;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "spug", matchIfMissing = true)
public class SpugSmsSender implements SmsSender {
    private static final String CODE_VALIDITY_MINUTES = "5";
    private final RestClient restClient;
    private final String loginUrl;
    private final String registerUrl;
    private final String senderName;

    public SpugSmsSender(
            RestClient.Builder restClientBuilder,
            @Value("${app.sms.spug.login-url:}") String loginUrl,
            @Value("${app.sms.spug.register-url:}") String registerUrl,
            @Value("${app.sms.spug.name:NexaFlow}") String senderName) {
        this.restClient = restClientBuilder.build();
        this.loginUrl = loginUrl;
        this.registerUrl = registerUrl;
        this.senderName = senderName;
    }

    @Override
    public void send(String phone, String code, String purpose) {
        URI endpoint = endpointFor(purpose);
        Map<String, String> payload = payloadFor(endpoint, phone, code);
        try {
            JsonNode response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            assertSuccessful(response);
        } catch (ResourceAccessException ex) {
            throw BusinessException.unavailable("无法连接短信平台，请检查网络后重试");
        } catch (RestClientResponseException ex) {
            throw BusinessException.upstream("短信平台请求失败（HTTP " + ex.getStatusCode().value() + "），请检查 Spug 推送记录");
        } catch (RestClientException ex) {
            throw BusinessException.upstream("短信平台响应格式异常，请检查 Spug 推送记录");
        }
    }

    private Map<String, String> payloadFor(URI endpoint, String phone, String code) {
        String target = spugTarget(phone);
        String verificationCode = required(code, "短信验证码");
        if (endpoint.getPath().startsWith("/sms/")) {
            return Map.of("code", verificationCode, "number", CODE_VALIDITY_MINUTES, "to", target);
        }
        return Map.of(
                "name", required(senderName, "短信发送名称"),
                "code", verificationCode,
                "targets", target);
    }

    private void assertSuccessful(JsonNode response) {
        if (response == null || response.isNull() || response.isMissingNode()) {
            return;
        }
        if (response.isTextual()) {
            String value = response.asText("").trim();
            if (value.equalsIgnoreCase("ok") || value.equalsIgnoreCase("success")) {
                return;
            }
            throw platformFailure(response);
        }
        if (response.has("error") && !isZeroOrFalse(response.get("error"))) {
            throw platformFailure(response);
        }
        if (response.has("success") && !response.path("success").asBoolean(false)) {
            throw platformFailure(response);
        }
        if (response.has("code") && !isSuccessCode(response.get("code"))) {
            throw platformFailure(response);
        }
    }

    private boolean isZeroOrFalse(JsonNode value) {
        return value.isBoolean() ? !value.asBoolean() : "0".equals(value.asText());
    }

    private boolean isSuccessCode(JsonNode value) {
        String code = value.asText("").trim();
        return "0".equals(code) || "200".equals(code)
                || "ok".equalsIgnoreCase(code) || "success".equalsIgnoreCase(code);
    }

    private BusinessException platformFailure(JsonNode response) {
        String message = firstText(response, "message", "msg", "detail");
        if (message.isBlank()) {
            return BusinessException.upstream("短信平台未受理发送请求，请检查 Spug 推送记录");
        }
        String safeMessage = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (safeMessage.length() > 100) {
            safeMessage = safeMessage.substring(0, 100);
        }
        return BusinessException.upstream("短信平台发送失败：" + safeMessage);
    }

    private String firstText(JsonNode response, String... names) {
        for (String name : names) {
            JsonNode value = response.get(name);
            if (value != null && value.isValueNode() && !value.asText("").isBlank()) {
                return value.asText();
            }
        }
        return "";
    }

    private URI endpointFor(String purpose) {
        String configuredUrl = switch (purpose) {
            case "LOGIN" -> endpointUrl(loginUrl, "Spug 登录短信模板地址");
            case "REGISTER" -> endpointUrl(registerUrl, "Spug 注册短信模板地址");
            default -> throw new BusinessException("验证码用途不合法");
        };
        URI endpoint;
        try {
            endpoint = URI.create(configuredUrl);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Spug 短信模板地址格式错误，请联系企业管理员");
        }
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null
                || endpoint.getPath() == null
                || (!endpoint.getPath().startsWith("/send/") && !endpoint.getPath().startsWith("/sms/"))) {
            throw new BusinessException("Spug 短信模板地址格式错误，请联系企业管理员");
        }
        return endpoint;
    }

    private String endpointUrl(String purposeUrl, String configurationName) {
        return required(purposeUrl, configurationName);
    }

    private String spugTarget(String phone) {
        String normalized = required(phone, "手机号");
        if (normalized.startsWith("+86") && normalized.length() == 14) {
            return normalized.substring(3);
        }
        return normalized;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(name + "尚未配置，请联系企业管理员");
        }
        return value.trim();
    }
}
