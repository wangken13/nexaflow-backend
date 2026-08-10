package com.vebcoding.trade.auth.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.vebcoding.trade.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun")
public class ConfiguredSmsSender implements SmsSender {
    @Value("${app.sms.endpoint:dysmsapi.aliyuncs.com}")
    private String endpoint;

    @Value("${app.sms.access-key-id:}")
    private String accessKeyId;

    @Value("${app.sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${app.sms.sign-name:}")
    private String signName;

    @Value("${app.sms.login-template-code:}")
    private String loginTemplateCode;

    @Value("${app.sms.register-template-code:}")
    private String registerTemplateCode;

    private volatile Client client;

    @Override
    public void send(String phone, String code, String purpose) {
        try {
            SendSmsResponse response = client().sendSms(new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(required(signName, "短信签名"))
                    .setTemplateCode(templateCode(purpose))
                    .setTemplateParam("{\"code\":\"" + code + "\"}"));
            String resultCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equalsIgnoreCase(resultCode)) {
                String resultMessage = response.getBody() == null ? "未返回失败原因" : response.getBody().getMessage();
                throw BusinessException.upstream("阿里云短信发送失败：" + safeMessage(resultMessage));
            }
        } catch (Exception ex) {
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw BusinessException.unavailable("无法连接阿里云短信服务，请检查网络和短信配置");
        }
    }

    private Client client() throws Exception {
        Client current = client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (client == null) {
                Config config = new Config()
                        .setAccessKeyId(required(accessKeyId, "AccessKey ID"))
                        .setAccessKeySecret(required(accessKeySecret, "AccessKey Secret"))
                        .setEndpoint(required(endpoint, "短信服务地址"));
                client = new Client(config);
            }
            return client;
        }
    }

    private String templateCode(String purpose) {
        return switch (purpose) {
            case "LOGIN" -> required(loginTemplateCode, "登录短信模板");
            case "REGISTER" -> required(registerTemplateCode, "注册短信模板");
            default -> throw new BusinessException("验证码用途不合法");
        };
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(name + "尚未配置，请联系企业管理员");
        }
        return value.trim();
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) return "未返回失败原因";
        String value = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return value.length() <= 100 ? value : value.substring(0, 100);
    }
}
