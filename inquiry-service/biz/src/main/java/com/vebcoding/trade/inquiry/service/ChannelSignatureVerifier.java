package com.vebcoding.trade.inquiry.service;

interface ChannelSignatureVerifier {
    void verify(String secret, String timestamp, String body, String suppliedSignature);
}
