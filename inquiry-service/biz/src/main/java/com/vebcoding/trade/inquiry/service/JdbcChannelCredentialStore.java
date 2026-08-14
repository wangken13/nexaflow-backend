package com.vebcoding.trade.inquiry.service;

import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.vebcoding.trade.inquiry.api.IntegrationInvocationView;

@Repository
class JdbcChannelCredentialStore implements ChannelCredentialStore {
    private final JdbcTemplate jdbcTemplate;

    JdbcChannelCredentialStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ChannelCredential save(ChannelCredential credential) {
        jdbcTemplate.update("""
                INSERT INTO inbound_channel_credentials
                  (id, tenant_id, display_name, channel_type, encrypted_secret, active_flag)
                VALUES (?, ?, ?, ?, ?, ?)
                """, credential.id(), credential.tenantId(), credential.displayName(), credential.channelType(),
                credential.encryptedSecret(), credential.active());
        return credential;
    }

    @Override
    public Optional<ChannelCredential> findActive(String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, tenant_id, display_name, channel_type, encrypted_secret, active_flag,
                           last_used_at, created_at
                    FROM inbound_channel_credentials WHERE id=? AND active_flag=1
                    """, (rs, rowNum) -> map(rs), id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public List<ChannelCredential> findByTenant(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, display_name, channel_type, encrypted_secret, active_flag,
                       last_used_at, created_at
                FROM inbound_channel_credentials WHERE tenant_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> map(rs), tenantId);
    }

    @Override
    public boolean revoke(String tenantId, String id) {
        return jdbcTemplate.update("""
                UPDATE inbound_channel_credentials SET active_flag=0 WHERE tenant_id=? AND id=? AND active_flag=1
                """, tenantId, id) > 0;
    }

    @Override
    public void markUsed(String id) {
        jdbcTemplate.update("UPDATE inbound_channel_credentials SET last_used_at=NOW() WHERE id=?", id);
    }

    @Override
    public Optional<InboundReceipt> findReceipt(String credentialId, String externalId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT tenant_id, credential_id, external_id, payload_hash, customer_id, inquiry_id, status
                    FROM inbound_message_receipts WHERE credential_id=? AND external_id=?
                    """, (rs, rowNum) -> new InboundReceipt(rs.getString("tenant_id"),
                    rs.getString("credential_id"), rs.getString("external_id"), rs.getString("payload_hash"),
                    rs.getString("customer_id"), rs.getString("inquiry_id"), rs.getString("status")),
                    credentialId, externalId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void saveReceipt(InboundReceipt receipt) {
        jdbcTemplate.update("""
                INSERT INTO inbound_message_receipts
                  (id, tenant_id, credential_id, external_id, payload_hash, customer_id, inquiry_id, status)
                VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?)
                """, receipt.tenantId(), receipt.credentialId(), receipt.externalId(), receipt.payloadHash(),
                receipt.customerId(), receipt.inquiryId(), receipt.status());
    }

    @Override
    public Optional<String> findTenantId(String credentialId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT tenant_id FROM inbound_channel_credentials WHERE id=?", String.class, credentialId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void saveInvocation(String tenantId, IntegrationInvocationView invocation) {
        jdbcTemplate.update("""
                INSERT INTO integration_invocation_logs
                  (id,tenant_id,credential_id,request_id,http_method,request_path,client_ip,outcome,duration_ms)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, invocation.id(), tenantId, invocation.credentialId(), invocation.requestId(),
                invocation.httpMethod(), invocation.requestPath(), invocation.clientIp(), invocation.outcome(),
                invocation.durationMs());
    }

    @Override
    public List<IntegrationInvocationView> findInvocations(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id,credential_id,request_id,http_method,request_path,client_ip,outcome,duration_ms,created_at
                FROM integration_invocation_logs WHERE tenant_id=? ORDER BY created_at DESC LIMIT 500
                """, (rs, rowNum) -> new IntegrationInvocationView(rs.getString("id"),
                rs.getString("credential_id"), rs.getString("request_id"), rs.getString("http_method"),
                rs.getString("request_path"), rs.getString("client_ip"), rs.getString("outcome"),
                rs.getLong("duration_ms"), timestampToIso(rs, "created_at")), tenantId);
    }

    private ChannelCredential map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ChannelCredential(rs.getString("id"), rs.getString("tenant_id"), rs.getString("display_name"),
                rs.getString("channel_type"), rs.getString("encrypted_secret"), rs.getBoolean("active_flag"),
                timestampToIso(rs, "last_used_at"), timestampToIso(rs, "created_at"));
    }
}
