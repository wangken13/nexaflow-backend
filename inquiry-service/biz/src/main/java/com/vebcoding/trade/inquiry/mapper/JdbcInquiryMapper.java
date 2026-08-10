package com.vebcoding.trade.inquiry.mapper;

import com.vebcoding.trade.inquiry.api.InquiryView;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.vebcoding.trade.common.JdbcValueSupport.isoToTimestamp;
import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

@Repository
public class JdbcInquiryMapper implements InquiryMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcInquiryMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<InquiryView> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, customer_id, subject, content, status, created_at
                FROM inquiries
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """, (rs, rowNum) -> new InquiryView(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("customer_id"),
                rs.getString("subject"),
                rs.getString("content"),
                rs.getString("status"),
                timestampToIso(rs, "created_at")), tenantId);
    }

    @Override
    public InquiryView save(InquiryView inquiry) {
        Timestamp createdAt = isoToTimestamp(inquiry.createdAt());
        jdbcTemplate.update("""
                INSERT INTO inquiries (id, tenant_id, customer_id, subject, content, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  customer_id = VALUES(customer_id),
                  subject = VALUES(subject),
                  content = VALUES(content),
                  status = VALUES(status)
                """,
                inquiry.id(),
                inquiry.tenantId(),
                inquiry.customerId(),
                inquiry.subject(),
                inquiry.content(),
                inquiry.status(),
                createdAt);
        return inquiry;
    }

    @Override
    public Optional<InquiryView> findByTenantIdAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, tenant_id, customer_id, subject, content, status, created_at
                    FROM inquiries
                    WHERE tenant_id = ? AND id = ?
                    """, (rs, rowNum) -> new InquiryView(
                    rs.getString("id"),
                    rs.getString("tenant_id"),
                    rs.getString("customer_id"),
                    rs.getString("subject"),
                    rs.getString("content"),
                    rs.getString("status"),
                    timestampToIso(rs, "created_at")), tenantId, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
