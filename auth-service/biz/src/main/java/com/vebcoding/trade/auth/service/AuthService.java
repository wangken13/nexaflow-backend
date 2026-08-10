package com.vebcoding.trade.auth.service;

import com.vebcoding.trade.auth.api.CurrentUserResponse;
import com.vebcoding.trade.auth.api.LoginRequest;
import com.vebcoding.trade.auth.api.LoginResponse;
import com.vebcoding.trade.auth.api.RegisterRequest;
import com.vebcoding.trade.auth.api.RegisterResponse;
import com.vebcoding.trade.auth.api.SmsCodeRequest;
import com.vebcoding.trade.auth.api.SmsCodeResponse;
import com.vebcoding.trade.auth.api.SmsLoginRequest;
import com.vebcoding.trade.auth.api.WechatAuthorizationResponse;
import com.vebcoding.trade.auth.mapper.AuthMapper;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.JwtSupport;
import com.vebcoding.trade.common.TenantContext;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int ACCESS_TOKEN_TTL_SECONDS = 900;
    private static final int REFRESH_TOKEN_TTL_SECONDS = 2_592_000;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int SMS_CODE_TTL_SECONDS = 300;
    private static final int MAX_SMS_SENDS_PER_WINDOW = 3;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,63}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final WechatOAuthService wechatOAuthService;
    private final LoginCaptchaVerifier loginCaptchaVerifier;
    private final AbuseProtectionService abuseProtectionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${JWT_SECRET}")
    private String secret;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this(authMapper, passwordEncoder, (phone, code, purpose) -> {
            throw new BusinessException("短信服务尚未配置，请联系企业管理员");
        }, null, (captchaId, captchaCode) -> false);
    }

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, SmsSender smsSender,
                       WechatOAuthService wechatOAuthService, LoginCaptchaVerifier loginCaptchaVerifier) {
        this(authMapper, passwordEncoder, smsSender, wechatOAuthService, loginCaptchaVerifier, null);
    }

    @Autowired
    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, SmsSender smsSender,
                       WechatOAuthService wechatOAuthService, LoginCaptchaVerifier loginCaptchaVerifier,
                       AbuseProtectionService abuseProtectionService) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
        this.wechatOAuthService = wechatOAuthService;
        this.loginCaptchaVerifier = loginCaptchaVerifier;
        this.abuseProtectionService = abuseProtectionService;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();
        if (!loginCaptchaVerifier.verify(request.captchaId(), request.captchaCode())) {
            throw new BusinessException("人机验证错误或已过期，请刷新后重试");
        }
        if (username.isBlank() || password.isBlank()
                || authMapper.countFailedAttemptsSince(username, Instant.now().minusSeconds(900)) >= MAX_FAILED_ATTEMPTS) {
            return Optional.empty();
        }
        Optional<AuthMapper.UserAccount> matched = authMapper.findByUsername(username)
                .filter(user -> passwordMatches(password, user));
        authMapper.recordLoginAttempt(username, matched.isPresent());
        return matched.map(this::loginResponse);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = requiredUsername(request.username());
        String password = requiredPassword(request.password());
        String phone = normalizedPhone(request.phone());
        if (authMapper.findByUsername(username).isPresent() || authMapper.findByPhone(phone).isPresent()) {
            throw BusinessException.conflict("账号或手机号已被使用");
        }
        if (!consumeVerificationCode(phone, "REGISTER", request.verificationCode())) {
            throw new BusinessException("验证码无效或已过期");
        }
        AuthMapper.UserAccount user = authMapper.createUser(
                requiredText(request.tenantName(), "企业名称", 128), username, passwordEncoder.encode(password),
                requiredText(request.displayName(), "姓名", 128), phone);
        log.info("auth.registration.succeeded userId={} tenantId={}", user.id(), user.tenantId());
        return new RegisterResponse(user.tenantId(), user.username());
    }

    public SmsCodeResponse sendSmsCode(SmsCodeRequest request) {
        return sendSmsCode(request, "unknown");
    }

    public SmsCodeResponse sendSmsCode(SmsCodeRequest request, String clientIp) {
        String purpose = normalizedPurpose(request.purpose());
        String phone = normalizedPhone(request.phone());
        if (abuseProtectionService != null) {
            abuseProtectionService.check("sms-ip", clientIp, 8, Duration.ofHours(1));
            abuseProtectionService.check("sms-phone", phone, 10, Duration.ofDays(1));
        }
        if (authMapper.countSmsCodesSince(phone, purpose, Instant.now().minusSeconds(600)) >= MAX_SMS_SENDS_PER_WINDOW) {
            throw BusinessException.rateLimited("验证码发送过于频繁，请 10 分钟后再试");
        }
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        smsSender.send(phone, code, purpose);
        authMapper.invalidateActiveSmsCodes(phone, purpose);
        authMapper.saveSmsCode(new AuthMapper.SmsCode("sms-" + UUID.randomUUID(), phone, purpose,
                passwordEncoder.encode(code), Instant.now().plusSeconds(SMS_CODE_TTL_SECONDS), 0));
        log.info("auth.sms_code.issued purpose={}", purpose);
        return new SmsCodeResponse(SMS_CODE_TTL_SECONDS);
    }

    @Transactional
    public Optional<LoginResponse> smsLogin(SmsLoginRequest request) {
        String phone = normalizedPhone(request.phone());
        if (!consumeVerificationCode(phone, "LOGIN", request.verificationCode())) {
            authMapper.recordLoginAttempt("sms:" + phone, false);
            return Optional.empty();
        }
        Optional<AuthMapper.UserAccount> user = authMapper.findByPhone(phone);
        authMapper.recordLoginAttempt("sms:" + phone, user.isPresent());
        if (user.isEmpty()) {
            throw BusinessException.notFound("该手机号尚未绑定企业账号，请先创建企业或联系管理员绑定手机号");
        }
        return user.map(this::loginResponse);
    }

    public WechatAuthorizationResponse wechatAuthorization() {
        if (wechatOAuthService == null) {
            throw new BusinessException("微信扫码登录尚未配置，请使用账号或手机号登录");
        }
        return wechatOAuthService.authorization();
    }

    public String completeWechatAuthorization(String code, String state) {
        if (wechatOAuthService == null) {
            throw new BusinessException("微信扫码登录尚未配置，请使用账号或手机号登录");
        }
        return wechatOAuthService.completeAuthorization(code, state);
    }

    public Optional<LoginResponse> wechatTicketLogin(String ticket) {
        if (wechatOAuthService == null) return Optional.empty();
        return wechatOAuthService.consumeLoginTicket(ticket).map(this::loginResponse);
    }

    public CurrentUserResponse currentUser() {
        return new CurrentUserResponse(TenantContext.userId(), TenantContext.tenantId(), TenantContext.role());
    }

    public String refreshToken(LoginResponse login) {
        return JwtSupport.createRefresh(login.userId(), login.sessionId(), secret,
                REFRESH_TOKEN_TTL_SECONDS);
    }

    @Transactional
    public Optional<LoginResponse> refresh(String refreshToken) {
        try {
            var claims = JwtSupport.verify(refreshToken, secret);
            if (!"REFRESH".equals(claims.get("typ"))) return Optional.empty();
            return authMapper.findActiveSession(claims.get("sid"), claims.get("sub"))
                    .flatMap(session -> authMapper.findById(session.userId()).map(user -> {
                        authMapper.revokeSession(session.id(), user.id());
                        return loginResponse(user);
                    }));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void logout() {
        authMapper.revokeSession(TenantContext.sessionId(), TenantContext.userId());
        log.info("auth.session.revoked userId={} sessionId={}", TenantContext.userId(), TenantContext.sessionId());
    }

    private boolean passwordMatches(String rawPassword, AuthMapper.UserAccount user) {
        return isBcryptHash(user.password()) && passwordEncoder.matches(rawPassword, user.password());
    }

    private boolean isBcryptHash(String password) {
        return password != null && password.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    private LoginResponse loginResponse(AuthMapper.UserAccount user) {
        String sessionId = "ses-" + UUID.randomUUID();
        authMapper.createSession(new AuthMapper.UserSession(sessionId, user.id(),
                Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS)));
        log.info("auth.session.issued userId={} tenantId={}", user.id(), user.tenantId());
        return new LoginResponse(JwtSupport.createAccess(user.id(), user.tenantId(), user.role(), sessionId, secret,
                ACCESS_TOKEN_TTL_SECONDS), user.id(), user.tenantId(), user.role(), user.username(), sessionId,
                ACCESS_TOKEN_TTL_SECONDS);
    }

    private boolean consumeVerificationCode(String phone, String purpose, String value) {
        Optional<AuthMapper.SmsCode> code = authMapper.findLatestActiveSmsCode(phone, purpose);
        if (code.isEmpty() || code.get().expiresAt().isBefore(Instant.now()) || code.get().attempts() >= MAX_FAILED_ATTEMPTS
                || value == null || !passwordEncoder.matches(value, code.get().codeHash())) {
            code.ifPresent(item -> authMapper.increaseSmsCodeAttempts(item.id()));
            return false;
        }
        return authMapper.consumeSmsCode(code.get().id());
    }

    private String normalizedPurpose(String purpose) {
        String value = purpose == null ? "" : purpose.trim().toUpperCase();
        if (!"LOGIN".equals(value) && !"REGISTER".equals(value)) {
            throw new BusinessException("验证码用途不合法");
        }
        return value;
    }

    private String normalizedPhone(String phone) {
        String compact = phone == null ? "" : phone.replaceAll("[\\s()-]", "");
        if (compact.matches("^1[3-9]\\d{9}$")) compact = "+86" + compact;
        if (!PHONE_PATTERN.matcher(compact).matches()) throw new BusinessException("请输入有效的手机号码");
        return compact;
    }

    private String requiredUsername(String username) {
        String value = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new BusinessException("账号需以字母开头，使用 4 至 64 位字母、数字或下划线");
        }
        return value;
    }

    private String requiredPassword(String password) {
        if (password == null || password.length() < 10 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException("密码至少 10 位，且需同时包含字母和数字");
        }
        return password;
    }

    private String requiredText(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) throw new BusinessException(field + "填写不合法");
        return normalized;
    }
}
