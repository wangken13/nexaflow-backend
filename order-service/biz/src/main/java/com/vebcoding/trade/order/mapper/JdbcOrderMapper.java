package com.vebcoding.trade.order.mapper;

import com.vebcoding.trade.order.api.OrderView;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.vebcoding.trade.common.JdbcValueSupport.isoToDate;

@Repository
public class JdbcOrderMapper implements OrderMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OrderView> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, customer_id, customer_name, product_id, product_name, status, delivery_date, risk
                FROM trade_orders
                WHERE tenant_id = ?
                ORDER BY created_at DESC
                """, (rs, rowNum) -> new OrderView(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("customer_id"),
                rs.getString("customer_name"),
                rs.getString("product_id"),
                rs.getString("product_name"),
                rs.getString("status"),
                rs.getDate("delivery_date").toLocalDate().toString(),
                rs.getBoolean("risk")), tenantId);
    }

    @Override
    public Optional<OrderView> findByTenantIdAndId(String tenantId, String id) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, customer_id, customer_name, product_id, product_name, status, delivery_date, risk
                FROM trade_orders WHERE tenant_id = ? AND id = ?
                """, (rs, rowNum) -> new OrderView(rs.getString("id"), rs.getString("tenant_id"),
                rs.getString("customer_id"), rs.getString("customer_name"), rs.getString("product_id"),
                rs.getString("product_name"), rs.getString("status"),
                rs.getDate("delivery_date").toLocalDate().toString(), rs.getBoolean("risk")), tenantId, id)
                .stream().findFirst();
    }

    @Override
    public OrderView save(OrderView order) {
        jdbcTemplate.update("""
                INSERT INTO trade_orders (id, tenant_id, customer_id, customer_name, product_id, product_name, status, delivery_date, risk)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  customer_id = VALUES(customer_id),
                  customer_name = VALUES(customer_name),
                  product_id = VALUES(product_id),
                  product_name = VALUES(product_name),
                  status = VALUES(status),
                  delivery_date = VALUES(delivery_date),
                  risk = VALUES(risk)
                """,
                order.id(),
                order.tenantId(),
                order.customerId(),
                order.customerName(),
                order.productId(),
                order.productName(),
                order.status(),
                isoToDate(order.deliveryDate()),
                order.risk());
        return order;
    }
}
