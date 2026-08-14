package com.vebcoding.trade.inquiry.api;

public record ChannelCredentialView(String id, String displayName, String channelType, String endpointPath,
                                    String signingSecret, boolean active, String lastUsedAt, String createdAt) {
}
