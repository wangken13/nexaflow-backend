package com.vebcoding.trade.customer.mapper;

import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.FollowupView;
import com.vebcoding.trade.customer.service.AssignableOwner;
import com.vebcoding.trade.customer.service.CustomerAccessProfile;
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
                SELECT customer.id, customer.tenant_id, customer.name, customer.country, customer.tag,
                       customer.owner_id, COALESCE(owner.display_name, '') owner_name,
                       customer.department_id, COALESCE(department.name, '') department_name, customer.created_at
                FROM customers customer
                LEFT JOIN users owner ON owner.tenant_id=customer.tenant_id AND owner.id=customer.owner_id
                LEFT JOIN departments department ON department.tenant_id=customer.tenant_id
                  AND department.id=customer.department_id
                WHERE customer.tenant_id = ? ORDER BY customer.created_at DESC
                """, (rs, rowNum) -> customer(rs), tenantId);
    }

    @Override
    public List<CustomerView> findByTenantIdAndOwnerId(String tenantId, String ownerId) {
        return jdbcTemplate.query("""
                SELECT customer.id, customer.tenant_id, customer.name, customer.country, customer.tag,
                       customer.owner_id, COALESCE(owner.display_name, '') owner_name,
                       customer.department_id, COALESCE(department.name, '') department_name, customer.created_at
                FROM customers customer
                LEFT JOIN users owner ON owner.tenant_id=customer.tenant_id AND owner.id=customer.owner_id
                LEFT JOIN departments department ON department.tenant_id=customer.tenant_id
                  AND department.id=customer.department_id
                WHERE customer.tenant_id=? AND customer.owner_id=? ORDER BY customer.created_at DESC
                """, (rs, rowNum) -> customer(rs), tenantId, ownerId);
    }

    @Override
    public List<CustomerView> findByTenantIdAndDepartmentId(String tenantId, String departmentId) {
        return jdbcTemplate.query("""
                SELECT customer.id, customer.tenant_id, customer.name, customer.country, customer.tag,
                       customer.owner_id, COALESCE(owner.display_name, '') owner_name,
                       customer.department_id, COALESCE(department.name, '') department_name, customer.created_at
                FROM customers customer
                LEFT JOIN users owner ON owner.tenant_id=customer.tenant_id AND owner.id=customer.owner_id
                LEFT JOIN departments department ON department.tenant_id=customer.tenant_id
                  AND department.id=customer.department_id
                WHERE customer.tenant_id=? AND customer.department_id=? ORDER BY customer.created_at DESC
                """, (rs, rowNum) -> customer(rs), tenantId, departmentId);
    }

    @Override
    public CustomerAccessProfile findAccessProfile(String tenantId, String userId, String fallbackRole) {
        return jdbcTemplate.query("""
                SELECT id, role_code, data_scope, department_id FROM users
                WHERE tenant_id=? AND id=? AND status='ACTIVE'
                """, (rs, rowNum) -> new CustomerAccessProfile(rs.getString("id"), rs.getString("role_code"),
                rs.getString("data_scope"), stringOrEmpty(rs, "department_id")), tenantId, userId).stream()
                .findFirst().orElse(new CustomerAccessProfile(userId, fallbackRole,
                        defaultScope(fallbackRole), ""));
    }

    @Override
    public Optional<AssignableOwner> findAssignableOwner(String tenantId, String userId) {
        return jdbcTemplate.query("""
                SELECT user.id, user.display_name, user.department_id, COALESCE(department.name, '') department_name
                FROM users user LEFT JOIN departments department
                  ON department.tenant_id=user.tenant_id AND department.id=user.department_id
                WHERE user.tenant_id=? AND user.id=? AND user.status='ACTIVE'
                """, (rs, rowNum) -> new AssignableOwner(rs.getString("id"), rs.getString("display_name"),
                stringOrEmpty(rs, "department_id"), rs.getString("department_name")), tenantId, userId)
                .stream().findFirst();
    }

    @Override
    public CustomerView save(CustomerView customer) {
        Timestamp createdAt = isoToTimestamp(customer.createdAt());
        jdbcTemplate.update("""
                INSERT INTO customers (id, tenant_id, name, country, tag, owner_id, department_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name),
                  country = VALUES(country),
                  tag = VALUES(tag),
                  owner_id = VALUES(owner_id),
                  department_id = VALUES(department_id)
                """,
                customer.id(),
                customer.tenantId(),
                customer.name(),
                blankToNull(customer.country()),
                blankToNull(customer.tag()),
                blankToNull(customer.ownerId()),
                blankToNull(customer.departmentId()),
                createdAt);
        return customer;
    }

    @Override
    public Optional<CustomerView> findByTenantIdAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT customer.id, customer.tenant_id, customer.name, customer.country, customer.tag,
                           customer.owner_id, COALESCE(owner.display_name, '') owner_name,
                           customer.department_id, COALESCE(department.name, '') department_name, customer.created_at
                    FROM customers customer
                    LEFT JOIN users owner ON owner.tenant_id=customer.tenant_id AND owner.id=customer.owner_id
                    LEFT JOIN departments department ON department.tenant_id=customer.tenant_id
                      AND department.id=customer.department_id
                    WHERE customer.tenant_id = ? AND customer.id = ?
                    """, (rs, rowNum) -> customer(rs), tenantId, id));
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

    private CustomerView customer(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CustomerView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("name"),
                stringOrEmpty(rs, "country"), stringOrEmpty(rs, "tag"), stringOrEmpty(rs, "owner_id"),
                rs.getString("owner_name"), stringOrEmpty(rs, "department_id"), rs.getString("department_name"),
                timestampToIso(rs, "created_at"));
    }

    private String defaultScope(String role) {
        if ("OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) return "ALL";
        if ("OPERATOR".equalsIgnoreCase(role)) return "DEPARTMENT";
        return "SELF";
    }
}
