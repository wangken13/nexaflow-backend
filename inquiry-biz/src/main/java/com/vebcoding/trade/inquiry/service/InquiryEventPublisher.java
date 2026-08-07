package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.inquiry.api.InquiryView;

public interface InquiryEventPublisher {
    void publishCreated(InquiryView inquiry);
}