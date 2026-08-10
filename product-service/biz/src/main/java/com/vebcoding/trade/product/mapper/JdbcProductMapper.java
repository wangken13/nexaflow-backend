package com.vebcoding.trade.product.mapper;

import static com.vebcoding.trade.common.JdbcValueSupport.blankToNull;
import static com.vebcoding.trade.common.JdbcValueSupport.isoToTimestamp;
import static com.vebcoding.trade.common.JdbcValueSupport.stringOrEmpty;
import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

import com.vebcoding.trade.product.api.ProductView;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProductMapper implements ProductMapper {
    private static final String COLUMNS = "id, tenant_id, sku, name, specification, currency, unit_price, moq, active_flag, created_at";
    private final JdbcTemplate jdbcTemplate;

    public JdbcProductMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ProductView> findByTenantId(String tenantId, String keyword) {
        String like = "%" + keyword + "%";
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM products WHERE tenant_id=? AND (sku LIKE ? OR name LIKE ?) ORDER BY active_flag DESC, created_at DESC",
                (rs, rowNum) -> map(rs), tenantId, like, like);
    }

    @Override
    public Optional<ProductView> findByTenantIdAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT " + COLUMNS + " FROM products WHERE tenant_id=? AND id=?",
                    (rs, rowNum) -> map(rs), tenantId, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public ProductView save(ProductView product) {
        jdbcTemplate.update("""
                INSERT INTO products (id, tenant_id, sku, name, specification, currency, unit_price, moq, active_flag, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE sku=VALUES(sku), name=VALUES(name), specification=VALUES(specification),
                  currency=VALUES(currency), unit_price=VALUES(unit_price), moq=VALUES(moq), active_flag=VALUES(active_flag)
                """, product.id(), product.tenantId(), product.sku(), product.name(), blankToNull(product.specification()),
                product.currency(), product.unitPrice(), product.moq(), product.active(), isoToTimestamp(product.createdAt()));
        return product;
    }

    @Override
    public boolean delete(String tenantId, String id) {
        return jdbcTemplate.update("DELETE FROM products WHERE tenant_id=? AND id=?", tenantId, id) > 0;
    }

    private ProductView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ProductView(rs.getString("id"), rs.getString("tenant_id"), rs.getString("sku"),
                rs.getString("name"), stringOrEmpty(rs, "specification"), rs.getString("currency"),
                rs.getBigDecimal("unit_price"), rs.getInt("moq"), rs.getBoolean("active_flag"),
                timestampToIso(rs, "created_at"));
    }
}
