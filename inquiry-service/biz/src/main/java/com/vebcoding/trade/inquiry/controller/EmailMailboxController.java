package com.vebcoding.trade.inquiry.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.inquiry.api.CreateEmailMailboxRequest;
import com.vebcoding.trade.inquiry.api.EmailMailboxView;
import com.vebcoding.trade.inquiry.service.EmailMailboxService;
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
@RequestMapping("/inquiry/email-mailboxes")
public class EmailMailboxController {
    private final EmailMailboxService mailboxService;

    public EmailMailboxController(EmailMailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @GetMapping
    public ApiResponse<List<EmailMailboxView>> list() {
        return ApiResponse.ok(mailboxService.list());
    }

    @PostMapping
    public ApiResponse<EmailMailboxView> create(@Valid @RequestBody CreateEmailMailboxRequest request) {
        return ApiResponse.ok(mailboxService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> disable(@PathVariable String id) {
        mailboxService.disable(id);
        return ApiResponse.ok(null);
    }
}
