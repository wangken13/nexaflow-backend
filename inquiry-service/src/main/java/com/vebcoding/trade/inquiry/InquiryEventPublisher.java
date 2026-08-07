package com.vebcoding.trade.inquiry;

public interface InquiryEventPublisher {
    void publishCreated(InquiryController.InquiryView inquiry);
}
