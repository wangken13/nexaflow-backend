package com.vebcoding.trade.tenant.api;

public record ChannelConfigView(String id, String channelType, String displayName, String accountRef,
                                boolean enabled, String connectionStatus, String updatedAt) {
}
