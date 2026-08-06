package com.vebcoding.trade.auth;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.JwtSupport;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Value("${app.jwt.secret:change-me-in-production}")
    private String secret;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!"admin".equals(request.username()) || !"admin123".equals(request.password())) {
            return ApiResponse.fail("用户名或密码错误");
        }
        String token = JwtSupport.create("admin", "demo-tenant", "OWNER", secret, 86_400);
        return ApiResponse.ok(new LoginResponse(token, "demo-tenant", "OWNER"));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, String>> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(Map.of("tenantId", "demo-tenant", "username", request.username()));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me() {
        return ApiResponse.ok(Map.of("username", "admin", "tenantId", "demo-tenant", "role", "OWNER"));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(@NotBlank String tenantName, @NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, String tenantId, String role) {
    }
}
