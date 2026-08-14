package com.vebcoding.trade.inquiry.service;

record ChannelCredential(String id, String tenantId, String displayName, String channelType,
                         String encryptedSecret, boolean active, String lastUsedAt, String createdAt) {
}
