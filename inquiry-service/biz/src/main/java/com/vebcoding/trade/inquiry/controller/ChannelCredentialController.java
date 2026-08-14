package com.vebcoding.trade.inquiry.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.inquiry.api.ChannelCredentialView;
import com.vebcoding.trade.inquiry.api.CreateChannelCredentialRequest;
import com.vebcoding.trade.inquiry.api.IntegrationInvocationView;
import com.vebcoding.trade.inquiry.service.ChannelCredentialService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inquiry/channel-credentials")
public class ChannelCredentialController {
    private final ChannelCredentialService credentialService;

    public ChannelCredentialController(ChannelCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public ApiResponse<List<ChannelCredentialView>> list() {
        return ApiResponse.ok(credentialService.list());
    }

    @PostMapping
    public ApiResponse<ChannelCredentialView> create(@Valid @RequestBody CreateChannelCredentialRequest request) {
        return ApiResponse.ok(credentialService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revoke(@PathVariable String id) {
        credentialService.revoke(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/invocations")
    public ApiResponse<List<IntegrationInvocationView>> invocations() {
        return ApiResponse.ok(credentialService.invocations());
    }
}
