package com.vebcoding.trade.auth.api;

import jakarta.validation.constraints.NotBlank;

public record WechatTicketLoginRequest(@NotBlank String ticket) {
}
