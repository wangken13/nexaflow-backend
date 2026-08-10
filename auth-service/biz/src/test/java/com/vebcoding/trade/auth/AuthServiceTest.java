package com.vebcoding.trade.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.RegisterRequest;
import com.vebcoding.trade.auth.api.SmsCodeRequest;
import com.vebcoding.trade.auth.api.SmsLoginRequest;
import com.vebcoding.trade.auth.mapper.InMemoryAuthMapper;
import com.vebcoding.trade.auth.service.AuthService;
import com.vebcoding.trade.auth.service.SmsSender;
import com.vebcoding.trade.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {
    @Test
    void smsCodeCanAuthenticateOnceAndCannotBeReplayed() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        RecordingSmsSender sender = new RecordingSmsSender();
        AuthService service = service(encoder, sender);

        service.sendSmsCode(new SmsCodeRequest("13800000000", "LOGIN"));

        assertThat(service.smsLogin(new SmsLoginRequest("13800000000", sender.code))).isPresent();
        assertThat(service.smsLogin(new SmsLoginRequest("13800000000", sender.code))).isEmpty();
    }

    @Test
    void registrationRequiresCodeAndProducesBcryptPasswordLogin() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        RecordingSmsSender sender = new RecordingSmsSender();
        AuthService service = service(encoder, sender);
        service.sendSmsCode(new SmsCodeRequest("13900000000", "REGISTER"));

        service.register(new RegisterRequest("Northstar Export", "northstar", "SecurePass2026", "王可", "13900000000", sender.code));

        assertThat(service.login(new LoginRequest("northstar", "SecurePass2026", "captcha", "captcha"))).isPresent();
    }

    @Test
    void registrationRejectsWeakPasswordBeforeCreatingTenant() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        RecordingSmsSender sender = new RecordingSmsSender();
        AuthService service = service(encoder, sender);

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "Northstar Export", "northstar", "short", "王可", "13900000000", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码至少 10 位");
    }

    @Test
    void smsLoginExplainsWhenVerifiedPhoneHasNoAccount() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        RecordingSmsSender sender = new RecordingSmsSender();
        AuthService service = service(encoder, sender);
        service.sendSmsCode(new SmsCodeRequest("13900000000", "LOGIN"));

        assertThatThrownBy(() -> service.smsLogin(new SmsLoginRequest("13900000000", sender.code)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该手机号尚未绑定企业账号，请先创建企业或联系管理员绑定手机号");
    }

    private AuthService service(BCryptPasswordEncoder encoder, SmsSender sender) {
        AuthService service = new AuthService(new InMemoryAuthMapper(encoder.encode("admin123")), encoder, sender, null,
                (captchaId, captchaCode) -> "captcha".equals(captchaId) && "captcha".equals(captchaCode));
        ReflectionTestUtils.setField(service, "secret", "test-secret-with-at-least-32-bytes-long");
        return service;
    }

    private static final class RecordingSmsSender implements SmsSender {
        private String code;

        @Override
        public void send(String phone, String code, String purpose) {
            this.code = code;
        }
    }
}
