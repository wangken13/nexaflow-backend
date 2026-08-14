package com.vebcoding.trade.inquiry.api;

public record EmailMailboxView(String id, String displayName, String emailAddress, String host, int port,
                               String username, String folder, boolean active, String connectionStatus,
                               String lastSyncAt, String lastError, String createdAt) {
}
