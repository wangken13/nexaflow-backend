package com.vebcoding.trade.quotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.vebcoding.trade")
public class QuotationApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuotationApplication.class, args);
    }
}
