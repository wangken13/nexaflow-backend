package com.vebcoding.trade.auth.controller;

import com.vebcoding.trade.auth.api.CurrentUserResponse;
import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.LoginCaptchaResponse;
import com.vebcoding.trade.auth.api.LoginResponse;
import com.vebcoding.trade.auth.api.RegisterRequest;
import com.vebcoding.trade.auth.api.RegisterResponse;
import com.vebcoding.trade.auth.api.SmsCodeRequest;
import com.vebcoding.trade.auth.api.SmsCodeResponse;
import com.vebcoding.trade.auth.api.SmsLoginRequest;
import com.vebcoding.trade.auth.api.WechatAuthorizationResponse;
import com.vebcoding.trade.auth.api.WechatTicketLoginRequest;
import com.vebcoding.trade.auth.service.AuthService;
import com.vebcoding.trade.auth.service.LoginCaptchaService;
import com.vebcoding.trade.auth.service.AbuseProtectionService;
import com.vebcoding.trade.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final LoginCaptchaService loginCaptchaService;
    private final AbuseProtectionService abuseProtectionService;

    @Value("${AUTH_COOKIE_SECURE:true}")
    private boolean secureCookie;

    public AuthController(AuthService authService, LoginCaptchaService loginCaptchaService) {
        this(authService, loginCaptchaService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(AuthService authService, LoginCaptchaService loginCaptchaService,
                          AbuseProtectionService abuseProtectionService) {
        this.authService = authService;
        this.loginCaptchaService = loginCaptchaService;
        this.abuseProtectionService = abuseProtectionService;
    }

    @GetMapping("/captcha")
    public ApiResponse<LoginCaptchaResponse> captcha(HttpServletRequest request, HttpServletResponse response) {
        protect("captcha-ip", request, 20, java.time.Duration.ofMinutes(10));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        return ApiResponse.ok(loginCaptchaService.issue());
    }

    public ApiResponse<LoginCaptchaResponse> captcha() {
        return ApiResponse.ok(loginCaptchaService.issue());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response,
                                            HttpServletRequest servletRequest) {
        protect("login-ip", servletRequest, 20, java.time.Duration.ofMinutes(15));
        return authService.login(request)
                .map(login -> ApiResponse.ok(withRefreshCookie(login, response)))
                .orElseThrow(() -> com.vebcoding.trade.common.BusinessException.unauthorized("用户名或密码错误"));
    }

    public ApiResponse<LoginResponse> login(LoginRequest request, HttpServletResponse response) {
        return authService.login(request).map(login -> ApiResponse.ok(withRefreshCookie(login, response)))
                .orElseThrow(() -> com.vebcoding.trade.common.BusinessException.unauthorized("用户名或密码错误"));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/sms-codes")
    public ApiResponse<SmsCodeResponse> sendSmsCode(@Valid @RequestBody SmsCodeRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.sendSmsCode(request, clientIp(servletRequest)));
    }

    @PostMapping("/sms-login")
    public ApiResponse<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest request, HttpServletResponse response,
                                               HttpServletRequest servletRequest) {
        protect("sms-login-ip", servletRequest, 20, java.time.Duration.ofMinutes(15));
        return authService.smsLogin(request)
                .map(login -> ApiResponse.ok(withRefreshCookie(login, response)))
                .orElseThrow(() -> com.vebcoding.trade.common.BusinessException.unauthorized("手机号或验证码错误"));
    }

    @GetMapping("/wechat/authorize")
    public ApiResponse<WechatAuthorizationResponse> wechatAuthorization() {
        return ApiResponse.ok(authService.wechatAuthorization());
    }

    @GetMapping("/wechat/callback")
    public ResponseEntity<Void> wechatCallback(@RequestParam String code, @RequestParam String state) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authService.completeWechatAuthorization(code, state))
                .build();
    }

    @PostMapping("/wechat/login")
    public ApiResponse<LoginResponse> wechatTicketLogin(@Valid @RequestBody WechatTicketLoginRequest request, HttpServletResponse response) {
        return authService.wechatTicketLogin(request.ticket())
                .map(login -> ApiResponse.ok(withRefreshCookie(login, response)))
                .orElseThrow(() -> com.vebcoding.trade.common.BusinessException.unauthorized("微信登录凭证无效或已过期，请重新扫码"));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@CookieValue(value = "NEXAFLOW_REFRESH", required = false) String refreshToken,
                                               HttpServletResponse response) {
        return authService.refresh(refreshToken == null ? "" : refreshToken)
                .map(login -> ApiResponse.ok(withRefreshCookie(login, response)))
                .orElseThrow(() -> com.vebcoding.trade.common.BusinessException.unauthorized("登录会话已失效，请重新登录"));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        authService.logout();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("").maxAge(0).build().toString());
        return ApiResponse.ok(null);
    }

    private LoginResponse withRefreshCookie(LoginResponse login, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(authService.refreshToken(login)).build().toString());
        return login;
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookie(String value) {
        return ResponseCookie.from("NEXAFLOW_REFRESH", value).httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path("/api/auth").maxAge(java.time.Duration.ofDays(30));
    }

    private void protect(String scope, HttpServletRequest request, int max, java.time.Duration window) {
        if (abuseProtectionService != null) abuseProtectionService.check(scope, clientIp(request), max, window);
    }

    private String clientIp(HttpServletRequest request) {
        return request == null || request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
