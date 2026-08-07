package com.vebcoding.trade.auth.controller;

import com.vebcoding.trade.auth.api.CurrentUserResponse;
import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.LoginResponse;
import com.vebcoding.trade.auth.api.RegisterRequest;
import com.vebcoding.trade.auth.api.RegisterResponse;
import com.vebcoding.trade.auth.service.AuthService;
import com.vebcoding.trade.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("鐢ㄦ埛鍚嶆垨瀵嗙爜閿欒"));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }
}