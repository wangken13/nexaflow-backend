package com.vebcoding.trade.inquiry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.vebcoding.trade")
public class InquiryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InquiryApplication.class, args);
    }
}
