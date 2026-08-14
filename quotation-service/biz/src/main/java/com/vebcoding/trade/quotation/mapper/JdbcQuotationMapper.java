package com.vebcoding.trade.quotation.mapper;

import static com.vebcoding.trade.common.JdbcValueSupport.blankToNull;
import static com.vebcoding.trade.common.JdbcValueSupport.isoToTimestamp;
import static com.vebcoding.trade.common.JdbcValueSupport.stringOrEmpty;
import static com.vebcoding.trade.common.JdbcValueSupport.timestampToIso;

import com.vebcoding.trade.quotation.api.QuotationItemView;
import com.vebcoding.trade.quotation.api.QuotationView;
import com.vebcoding.trade.quotation.api.QuotationApprovalView;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuotationMapper implements QuotationMapper {
    private final JdbcTemplate jdbcTemplate;
    public JdbcQuotationMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public List<QuotationView> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                SELECT id, tenant_id, customer_id, quotation_no, product_name, quantity, unit_price, currency,
                  trade_term, destination_port, freight, valid_until, notes, status, created_at
                FROM quotations WHERE tenant_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> mapHeader(rs), tenantId);
    }

    @Override
    public Optional<QuotationView> findByTenantIdAndId(String tenantId, String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, tenant_id, customer_id, quotation_no, product_name, quantity, unit_price, currency,
                      trade_term, destination_port, freight, valid_until, notes, status, created_at
                    FROM quotations WHERE tenant_id=? AND id=?
                    """, (rs, rowNum) -> mapHeader(rs), tenantId, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public QuotationView save(QuotationView quotation) {
        jdbcTemplate.update("""
                INSERT INTO quotations (id, tenant_id, customer_id, product_name, quantity, unit_price, status,
                  quotation_no, currency, trade_term, destination_port, freight, valid_until, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE customer_id=VALUES(customer_id), product_name=VALUES(product_name),
                  quantity=VALUES(quantity), unit_price=VALUES(unit_price), status=VALUES(status),
                  quotation_no=VALUES(quotation_no), currency=VALUES(currency), trade_term=VALUES(trade_term),
                  destination_port=VALUES(destination_port), freight=VALUES(freight), valid_until=VALUES(valid_until),
                  notes=VALUES(notes)
                """, quotation.id(), quotation.tenantId(), quotation.customerId(), quotation.productName(),
                quotation.quantity(), quotation.unitPrice(), quotation.status(), quotation.quotationNo(),
                quotation.currency(), quotation.tradeTerm(), blankToNull(quotation.destinationPort()),
                quotation.freight(), toDate(quotation.validUntil()), blankToNull(quotation.notes()),
                isoToTimestamp(quotation.createdAt()));
        jdbcTemplate.update("DELETE FROM quotation_items WHERE tenant_id=? AND quotation_id=?",
                quotation.tenantId(), quotation.id());
        int sort = 0;
        for (QuotationItemView item : quotation.items()) {
            jdbcTemplate.update("""
                    INSERT INTO quotation_items (id, tenant_id, quotation_id, product_id, product_name,
                      specification, quantity, unit_price, amount, sort_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, item.id(), quotation.tenantId(), quotation.id(), blankToNull(item.productId()),
                    item.productName(), blankToNull(item.specification()), item.quantity(), item.unitPrice(),
                    item.amount(), sort++);
        }
        return quotation;
    }

    @Override
    public List<QuotationApprovalView> findApprovals(String tenantId, String quotationId) {
        return jdbcTemplate.query("""
                SELECT id, quotation_id, action, comment_text, operator_id, created_at
                FROM quotation_approval_records WHERE tenant_id=? AND quotation_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> new QuotationApprovalView(rs.getString("id"), rs.getString("quotation_id"),
                rs.getString("action"), stringOrEmpty(rs, "comment_text"), rs.getString("operator_id"),
                rs.getTimestamp("created_at").toInstant().toString()), tenantId, quotationId);
    }

    @Override
    public QuotationApprovalView saveApproval(String tenantId, QuotationApprovalView approval) {
        jdbcTemplate.update("""
                INSERT INTO quotation_approval_records
                  (id, tenant_id, quotation_id, action, comment_text, operator_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, approval.id(), tenantId, approval.quotationId(), approval.action(),
                blankToNull(approval.comment()), approval.operatorId(), isoToTimestamp(approval.createdAt()));
        return approval;
    }

    private QuotationView mapHeader(java.sql.ResultSet rs) throws java.sql.SQLException {
        String tenantId = rs.getString("tenant_id");
        String id = rs.getString("id");
        List<QuotationItemView> items = jdbcTemplate.query("""
                SELECT id, product_id, product_name, specification, quantity, unit_price, amount
                FROM quotation_items WHERE tenant_id=? AND quotation_id=? ORDER BY sort_no
                """, (itemRs, rowNum) -> new QuotationItemView(itemRs.getString("id"),
                stringOrEmpty(itemRs, "product_id"), itemRs.getString("product_name"),
                stringOrEmpty(itemRs, "specification"), itemRs.getInt("quantity"),
                itemRs.getBigDecimal("unit_price"), itemRs.getBigDecimal("amount")), tenantId, id);
        BigDecimal freight = rs.getBigDecimal("freight");
        if (freight == null) freight = BigDecimal.ZERO;
        BigDecimal total = items.stream().map(QuotationItemView::amount).reduce(BigDecimal.ZERO, BigDecimal::add).add(freight);
        Date validUntil = rs.getDate("valid_until");
        return new QuotationView(id, tenantId, rs.getString("customer_id"), stringOrEmpty(rs, "quotation_no"),
                rs.getString("product_name"), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                rs.getString("currency"), rs.getString("trade_term"), stringOrEmpty(rs, "destination_port"),
                freight, total, validUntil == null ? "" : validUntil.toLocalDate().toString(),
                stringOrEmpty(rs, "notes"), rs.getString("status"), items, timestampToIso(rs, "created_at"));
    }

    private Date toDate(String value) {
        return value == null || value.isBlank() ? null : Date.valueOf(LocalDate.parse(value));
    }
}
