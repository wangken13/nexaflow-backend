package com.vebcoding.trade.auth.api;

public record LoginCaptchaResponse(String captchaId, String imageDataUrl, int expiresInSeconds) {
}
