package com.vebcoding.trade.auth.service;

public interface LoginCaptchaVerifier {
    boolean verify(String captchaId, String captchaCode);
}
