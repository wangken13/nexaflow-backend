package com.vebcoding.trade.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthControllerTest {
    @Test
    void loginReturnsTokenForDemoUser() {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "secret", "test-secret");

        ApiResponse<AuthController.LoginResponse> response =
                controller.login(new AuthController.LoginRequest("admin", "admin123"));

        assertThat(response.success()).isTrue();
        assertThat(response.data().token()).isNotBlank();
    }
}
