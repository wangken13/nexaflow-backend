package com.vebcoding.trade.auth.service;

public interface SmsSender {
    void send(String phone, String code, String purpose);
}
