package com.vebcoding.trade.inquiry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.inquiry.api.InboundInquiryRequest;
import com.vebcoding.trade.inquiry.api.InboundInquiryResponse;
import com.vebcoding.trade.inquiry.api.InquiryView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundInquiryService {
    private static final int MAX_BODY_BYTES = 256 * 1024;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final ChannelCredentialStore credentialStore;
    private final SecretProtector secretProtector;
    private final ChannelSignatureVerifier signatureVerifier;
    private final InboundCustomerResolver customerResolver;
    private final InquiryService inquiryService;
    private final ObjectMapper objectMapper;

    public InboundInquiryService(ChannelCredentialStore credentialStore, SecretProtector secretProtector,
                                 ChannelSignatureVerifier signatureVerifier,
                                 InboundCustomerResolver customerResolver, InquiryService inquiryService,
                                 ObjectMapper objectMapper) {
        this.credentialStore = credentialStore;
        this.secretProtector = secretProtector;
        this.signatureVerifier = signatureVerifier;
        this.customerResolver = customerResolver;
        this.inquiryService = inquiryService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InboundInquiryResponse accept(String credentialId, String timestamp, String signature, String body) {
        if (body == null || body.isBlank()) throw new BusinessException("渠道请求内容不能为空");
        if (body.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) {
            throw new BusinessException("渠道请求内容不能超过256KB");
        }
        ChannelCredential credential = credentialStore.findActive(credentialId)
                .orElseThrow(() -> BusinessException.unauthorized("渠道接入凭据无效或已停用"));
        signatureVerifier.verify(secretProtector.reveal(credential.encryptedSecret()), timestamp, body, signature);
        InboundInquiryRequest request = parse(body);
        return process(credentialId, credential.tenantId(), credential.channelType(), request, body);
    }

    InboundInquiryResponse acceptTrusted(String sourceId, String tenantId, String channelType,
                                         InboundInquiryRequest request) {
        String canonicalPayload;
        try {
            canonicalPayload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize trusted channel message", exception);
        }
        return process(sourceId, tenantId, channelType, request, canonicalPayload);
    }

    private InboundInquiryResponse process(String credentialId, String tenantId, String channelType,
                                           InboundInquiryRequest request, String payload) {
        String externalId = limited(request.externalId(), "外部消息编号", 128);
        InboundReceipt existing = credentialStore.findReceipt(credentialId, externalId).orElse(null);
        if (existing != null) return duplicate(existing);

        validate(request);
        InboundCustomerResolver.ResolvedCustomer customer = customerResolver.resolve(tenantId, request);
        existing = credentialStore.findReceipt(credentialId, externalId).orElse(null);
        if (existing != null) return duplicate(existing);

        InquiryView inquiry = inquiryService.createFromChannel(tenantId, customer.customerId(),
                limited(request.subject(), "询盘主题", 255), limited(request.content(), "询盘内容", 20_000),
                channelType, externalId);
        credentialStore.saveReceipt(new InboundReceipt(tenantId, credentialId, externalId,
                sha256(payload), customer.customerId(), inquiry.id(), "ACCEPTED"));
        credentialStore.markUsed(credentialId);
        return new InboundInquiryResponse(inquiry.id(), customer.customerId(), false, inquiry.status());
    }

    private InboundInquiryRequest parse(String body) {
        try {
            return objectMapper.readValue(body, InboundInquiryRequest.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("渠道请求不是有效的 JSON 数据");
        }
    }

    private void validate(InboundInquiryRequest request) {
        limited(request.customerName(), "客户名称", 128);
        limited(request.contactName(), "联系人姓名", 128, false);
        String email = limited(request.email(), "联系人邮箱", 255, false).toLowerCase(Locale.ROOT);
        if (!email.isBlank() && !EMAIL.matcher(email).matches()) throw new BusinessException("联系人邮箱格式错误");
        limited(request.phone(), "联系人电话", 64, false);
        limited(request.country(), "国家或地区", 64, false);
    }

    private String limited(String value, String field, int maxLength) {
        return limited(value, field, maxLength, true);
    }

    private String limited(String value, String field, int maxLength, boolean required) {
        String normalized = required ? TextSanitizer.required(value, field) : TextSanitizer.optional(value);
        if (normalized.length() > maxLength) throw new BusinessException(field + "不能超过" + maxLength + "个字符");
        return normalized;
    }

    private InboundInquiryResponse duplicate(InboundReceipt receipt) {
        return new InboundInquiryResponse(receipt.inquiryId(), receipt.customerId(), true, receipt.status());
    }

    private String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
