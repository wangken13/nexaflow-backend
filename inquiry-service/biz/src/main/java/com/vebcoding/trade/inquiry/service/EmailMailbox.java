package com.vebcoding.trade.inquiry.service;

record EmailMailbox(String id, String tenantId, String displayName, String emailAddress, String host, int port,
                    String username, String encryptedPassword, String folder, boolean active,
                    String connectionStatus, String lastSyncAt, String lastError, String createdAt) {
}
