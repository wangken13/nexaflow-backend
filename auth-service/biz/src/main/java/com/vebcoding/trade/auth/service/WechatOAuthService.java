package com.vebcoding.trade.auth.service;

import com.vebcoding.trade.auth.api.WechatAuthorizationResponse;
import com.vebcoding.trade.auth.mapper.AuthMapper;
import com.vebcoding.trade.auth.mapper.WechatOAuthMapper;
import com.vebcoding.trade.common.BusinessException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WechatOAuthService {
    private static final int STATE_TTL_SECONDS = 300;
    private static final int TICKET_TTL_SECONDS = 60;
    private static final String PROVIDER = "WECHAT";

    private final WechatOAuthMapper wechatOAuthMapper;
    private final RestClient restClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.wechat.open-app-id:}")
    private String appId;

    @Value("${app.wechat.open-app-secret:}")
    private String appSecret;

    @Value("${app.wechat.redirect-uri:}")
    private String redirectUri;

    @Value("${app.wechat.frontend-login-uri:http://localhost:5173/login}")
    private String frontendLoginUri;

    @Autowired
    public WechatOAuthService(WechatOAuthMapper wechatOAuthMapper) {
        this(wechatOAuthMapper, RestClient.create());
    }

    WechatOAuthService(WechatOAuthMapper wechatOAuthMapper, RestClient restClient) {
        this.wechatOAuthMapper = wechatOAuthMapper;
        this.restClient = restClient;
    }

    public WechatAuthorizationResponse authorization() {
        validateConfiguration();
        Instant now = Instant.now();
        String state = nextState();
        wechatOAuthMapper.purgeExpired(now.minusSeconds(86_400));
        wechatOAuthMapper.saveState(state, now.plusSeconds(STATE_TTL_SECONDS));
        return new WechatAuthorizationResponse("https://open.weixin.qq.com/connect/qrconnect?appid=" + encode(appId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code&scope=snsapi_login&state=" + state + "#wechat_redirect");
    }

    public String completeAuthorization(String code, String state) {
        validateConfiguration();
        if (code == null || code.isBlank() || state == null || !wechatOAuthMapper.consumeState(state)) {
            throw BusinessException.unauthorized("微信授权已失效，请重新扫码登录");
        }
        Map<String, Object> token = request("https://api.weixin.qq.com/sns/oauth2/access_token", Map.of(
                "appid", appId, "secret", appSecret, "code", code, "grant_type", "authorization_code"));
        String accessToken = required(token, "access_token", "微信授权失败，请重新扫码");
        String openId = required(token, "openid", "微信授权失败，请重新扫码");
        Map<String, Object> profile = request("https://api.weixin.qq.com/sns/userinfo", Map.of(
                "access_token", accessToken, "openid", openId, "lang", "zh_CN"));
        String providerSubject = optional(profile, "unionid");
        if (providerSubject.isBlank()) providerSubject = openId;

        AuthMapper.UserAccount user = wechatOAuthMapper.findActiveUserByIdentity(PROVIDER, providerSubject)
                .orElseThrow(() -> BusinessException.notFound("该微信尚未绑定企业账号，请使用账号登录后联系管理员绑定"));
        String ticket = "wechat-ticket-" + UUID.randomUUID();
        wechatOAuthMapper.saveLoginTicket(ticket, user.id(), Instant.now().plusSeconds(TICKET_TTL_SECONDS));
        return appendTicket(frontendLoginUri, ticket);
    }

    public Optional<AuthMapper.UserAccount> consumeLoginTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) return Optional.empty();
        Optional<AuthMapper.UserAccount> user = wechatOAuthMapper.findActiveUserByTicket(ticket, Instant.now());
        if (user.isEmpty()) return Optional.empty();
        return wechatOAuthMapper.consumeLoginTicket(ticket) ? user : Optional.empty();
    }

    private Map<String, Object> request(String endpoint, Map<String, String> parameters) {
        String query = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right).orElse("");
        try {
            Map<String, Object> body = restClient.get().uri(endpoint + "?" + query).retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            if (body == null || body.containsKey("errcode")) {
                throw BusinessException.upstream("微信平台拒绝了授权请求，请重新扫码后再试");
            }
            return body;
        } catch (RestClientException exception) {
            throw BusinessException.unavailable("无法连接微信平台，请检查网络后重试");
        }
    }

    private void validateConfiguration() {
        if (isBlank(appId) || isBlank(appSecret) || isBlank(redirectUri) || isBlank(frontendLoginUri)) {
            throw new BusinessException("微信扫码登录尚未完成配置，请联系企业管理员");
        }
    }

    private String nextState() {
        byte[] stateBytes = new byte[24];
        secureRandom.nextBytes(stateBytes);
        return HexFormat.of().formatHex(stateBytes);
    }

    private String appendTicket(String target, String ticket) {
        return target + (target.contains("?") ? "&" : "?") + "wechatLoginTicket=" + encode(ticket);
    }

    private String required(Map<String, Object> values, String key, String message) {
        String value = optional(values, key);
        if (value.isBlank()) throw new BusinessException(message);
        return value;
    }

    private String optional(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
