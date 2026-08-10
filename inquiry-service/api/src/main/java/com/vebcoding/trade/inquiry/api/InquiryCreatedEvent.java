package com.vebcoding.trade.inquiry.api;

import java.io.Serializable;

public record InquiryCreatedEvent(
    String eventId,
    String inquiryId,
    String tenantId,
    String content
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
