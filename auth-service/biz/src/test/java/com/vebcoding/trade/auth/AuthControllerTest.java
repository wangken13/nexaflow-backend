package com.vebcoding.trade.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.LoginResponse;
import com.vebcoding.trade.auth.controller.AuthController;
import com.vebcoding.trade.auth.mapper.InMemoryAuthMapper;
import com.vebcoding.trade.auth.service.AuthService;
import com.vebcoding.trade.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthControllerTest {
    @Test
    void loginReturnsTokenForDemoUser() {
        AuthService service = new AuthService(new InMemoryAuthMapper());
        ReflectionTestUtils.setField(service, "secret", "test-secret");
        AuthController controller = new AuthController(service);

        ApiResponse<LoginResponse> response = controller.login(new LoginRequest("admin", "admin123"));

        assertThat(response.success()).isTrue();
        assertThat(response.data().token()).isNotBlank();
    }
}
