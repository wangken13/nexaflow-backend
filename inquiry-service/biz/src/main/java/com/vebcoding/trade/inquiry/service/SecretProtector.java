package com.vebcoding.trade.inquiry.service;

interface SecretProtector {
    String protect(String secret);
    String reveal(String protectedSecret);
    boolean available();
}
