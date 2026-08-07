package com.vebcoding.trade.auth.service;

import com.vebcoding.trade.auth.api.CurrentUserResponse;
import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.LoginResponse;
import com.vebcoding.trade.auth.api.RegisterRequest;
import com.vebcoding.trade.auth.api.RegisterResponse;
import com.vebcoding.trade.auth.mapper.AuthMapper;
import com.vebcoding.trade.common.JwtSupport;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthMapper authMapper;

    @Value("${app.jwt.secret:change-me-in-production}")
    private String secret;

    public AuthService(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        return authMapper.findByUsername(request.username())
                .filter(user -> user.password().equals(request.password()))
                .map(user -> new LoginResponse(
                        JwtSupport.create(user.username(), user.tenantId(), user.role(), secret, 86_400),
                        user.tenantId(),
                        user.role()));
    }

    public RegisterResponse register(RegisterRequest request) {
        AuthMapper.UserAccount user = authMapper.createUser(request.tenantName(), request.username(), request.password());
        return new RegisterResponse(user.tenantId(), user.username());
    }

    public CurrentUserResponse currentUser() {
        return new CurrentUserResponse("admin", "demo-tenant", "OWNER");
    }
}