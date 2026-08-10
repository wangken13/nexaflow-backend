package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.FollowupView;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.vebcoding.trade.common.JdbcValueSupport.blankToNull;
import static com.vebcoding.trade.common.JdbcValueSupport.isoToTimestamp;
import static com.vebcoding.trade.common.JdbcValueSupport.stringOrEmpty;
import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

@Repository
public class JdbcCustomerMapper implements CustomerMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCustomerMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CustomerView> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, name, country, tag, created_at
                FROM customers
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """, (rs, rowNum) -> new CustomerView(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("name"),
                stringOrEmpty(rs, "country"),
                stringOrEmpty(rs, "tag"),
                timestampToIso(rs, "created_at")), tenantId);
    }

    @Override
    public CustomerView save(CustomerView customer) {
        Timestamp createdAt = isoToTimestamp(customer.createdAt());
        jdbcTemplate.update("""
                INSERT INTO customers (id, tenant_id, name, country, tag, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name),
                  country = VALUES(country),
                  tag = VALUES(tag)
                """,
                customer.id(),
                customer.tenantId(),
                customer.name(),
                blankToNull(customer.country()),
                blankToNull(customer.tag()),
                createdAt);
        return customer;
    }

    @Override
    public Optional<CustomerView> findByTenantIdAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, tenant_id, name, country, tag, created_at FROM customers
                    WHERE tenant_id = ? AND id = ?
                    """, (rs, rowNum) -> new CustomerView(rs.getString("id"), rs.getString("tenant_id"),
                    rs.getString("name"), stringOrEmpty(rs, "country"), stringOrEmpty(rs, "tag"),
                    timestampToIso(rs, "created_at")), tenantId, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String tenantId, String id) {
        jdbcTemplate.update("DELETE FROM customer_contacts WHERE tenant_id=? AND customer_id=?", tenantId, id);
        jdbcTemplate.update("DELETE FROM customer_followups WHERE tenant_id=? AND customer_id=?", tenantId, id);
        return jdbcTemplate.update("DELETE FROM customers WHERE tenant_id=? AND id=?", tenantId, id) > 0;
    }

    @Override
    public List<ContactView> findContacts(String tenantId, String customerId) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, name, email, phone, position, primary_flag, created_at
                FROM customer_contacts WHERE tenant_id=? AND customer_id=?
                ORDER BY primary_flag DESC, created_at ASC
                """, (rs, rowNum) -> new ContactView(rs.getString("id"), rs.getString("customer_id"),
                rs.getString("name"), stringOrEmpty(rs, "email"), stringOrEmpty(rs, "phone"),
                stringOrEmpty(rs, "position"), rs.getBoolean("primary_flag"), timestampToIso(rs, "created_at")),
                tenantId, customerId);
    }

    @Override
    public ContactView saveContact(String tenantId, ContactView contact) {
        if (contact.primary()) {
            jdbcTemplate.update("UPDATE customer_contacts SET primary_flag=0 WHERE tenant_id=? AND customer_id=?",
                    tenantId, contact.customerId());
        }
        jdbcTemplate.update("""
                INSERT INTO customer_contacts (id, tenant_id, customer_id, name, email, phone, position, primary_flag, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, contact.id(), tenantId, contact.customerId(), contact.name(), blankToNull(contact.email()),
                blankToNull(contact.phone()), blankToNull(contact.position()), contact.primary(),
                isoToTimestamp(contact.createdAt()));
        return contact;
    }

    @Override
    public List<FollowupView> findFollowups(String tenantId, String customerId) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, followup_type, content, operator_name, created_at
                FROM customer_followups WHERE tenant_id=? AND customer_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> new FollowupView(rs.getString("id"), rs.getString("customer_id"),
                rs.getString("followup_type"), rs.getString("content"), stringOrEmpty(rs, "operator_name"),
                timestampToIso(rs, "created_at")), tenantId, customerId);
    }

    @Override
    public FollowupView saveFollowup(String tenantId, FollowupView followup) {
        jdbcTemplate.update("""
                INSERT INTO customer_followups (id, tenant_id, customer_id, followup_type, content, operator_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, followup.id(), tenantId, followup.customerId(), followup.type(), followup.content(),
                blankToNull(followup.operatorName()), isoToTimestamp(followup.createdAt()));
        return followup;
    }
}
