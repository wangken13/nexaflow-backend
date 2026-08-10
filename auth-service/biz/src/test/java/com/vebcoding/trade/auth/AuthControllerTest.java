package com.vebcoding.trade.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.LoginResponse;
import com.vebcoding.trade.auth.controller.AuthController;
import com.vebcoding.trade.auth.mapper.InMemoryAuthMapper;
import com.vebcoding.trade.auth.mapper.TestLoginCaptchaMapper;
import com.vebcoding.trade.auth.service.AuthService;
import com.vebcoding.trade.auth.service.LoginCaptchaService;
import com.vebcoding.trade.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthControllerTest {
    @Test
    void loginReturnsTokenForDemoUser() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(new InMemoryAuthMapper(
                passwordEncoder.encode("admin123")), passwordEncoder, (phone, code, purpose) -> { }, null,
                (captchaId, captchaCode) -> true);
        ReflectionTestUtils.setField(service, "secret", "test-secret-with-at-least-32-bytes-long");
        AuthController controller = new AuthController(service,
                new LoginCaptchaService(new TestLoginCaptchaMapper(), passwordEncoder));

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ApiResponse<LoginResponse> response = controller.login(new LoginRequest("admin", "admin123", "captcha", "captcha"), servletResponse);

        assertThat(response.success()).isTrue();
        assertThat(response.data().token()).isNotBlank();
        assertThat(servletResponse.getHeader("Set-Cookie")).contains("HttpOnly");
        assertThat(controller.captcha().data().imageDataUrl()).startsWith("data:image/png;base64,");
    }
}
