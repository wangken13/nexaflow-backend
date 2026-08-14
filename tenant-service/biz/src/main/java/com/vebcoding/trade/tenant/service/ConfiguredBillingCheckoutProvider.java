package com.vebcoding.trade.tenant.service;

import com.vebcoding.trade.tenant.api.SubscriptionOrderView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredBillingCheckoutProvider implements BillingCheckoutProvider {
    private final String baseUrl;
    private final String signingSecret;

    public ConfiguredBillingCheckoutProvider(@Value("${app.billing.checkout-base-url:}") String baseUrl,
                                             @Value("${app.billing.webhook-secret:}") String signingSecret) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.signingSecret = signingSecret == null ? "" : signingSecret.trim();
    }

    public String checkoutUrl(SubscriptionOrderView order) {
        if (baseUrl.isBlank() || signingSecret.length() < 32) return "";
        String payload = order.id() + "|" + order.amount().toPlainString() + "|" + order.expiresAt();
        return baseUrl + (baseUrl.contains("?") ? "&" : "?")
                + "orderId=" + encode(order.id()) + "&amount=" + encode(order.amount().toPlainString())
                + "&expiresAt=" + encode(order.expiresAt()) + "&signature=" + encode(hmac(payload));
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign checkout request", exception);
        }
    }
}
