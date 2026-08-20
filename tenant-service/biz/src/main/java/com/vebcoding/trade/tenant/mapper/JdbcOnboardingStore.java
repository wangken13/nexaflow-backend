package com.vebcoding.trade.tenant.mapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOnboardingStore implements OnboardingStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOnboardingStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SetupCounts counts(String tenantId) {
        return new SetupCounts(
                count("SELECT GREATEST(COUNT(*) - 1, 0) FROM users WHERE tenant_id=? AND status='ACTIVE'", tenantId),
                count("SELECT COUNT(*) FROM customers WHERE tenant_id=?", tenantId),
                count("SELECT COUNT(*) FROM products WHERE tenant_id=? AND active_flag=1", tenantId),
                count("SELECT COUNT(*) FROM email_mailbox_configs WHERE tenant_id=? AND active_flag=1", tenantId),
                count("SELECT COUNT(*) FROM inquiries WHERE tenant_id=?", tenantId));
    }

    @Override
    public boolean hasDemoData(String tenantId) {
        return count("SELECT COUNT(*) FROM tenant_demo_records WHERE tenant_id=?", tenantId) > 0;
    }

    @Override
    public void createDemoData(String tenantId, String userId, DemoIds ids) {
        String departmentId = ids.batchId() + "-department";
        String knowledgeId = ids.batchId() + "-knowledge";
        String approvalRuleId = ids.batchId() + "-approval-rule";
        jdbcTemplate.update("INSERT INTO departments (id,tenant_id,name,parent_id,status) VALUES (?,?,?,?,?)",
                departmentId, tenantId, "演示运营团队-" + ids.batchId().substring(ids.batchId().length() - 6), null, "ACTIVE");
        jdbcTemplate.update("UPDATE users SET department_id=?, data_scope='ALL' WHERE tenant_id=? AND id=?",
                departmentId, tenantId, userId);
        jdbcTemplate.update("INSERT INTO customers (id,tenant_id,name,country,tag,owner_id) VALUES (?,?,?,?,?,?)",
                ids.customerId(), tenantId, "演示客户｜Northstar Retail", "United States", "重点客户", userId);
        jdbcTemplate.update("INSERT INTO products (id,tenant_id,sku,name,specification,currency,unit_price,moq,active_flag) VALUES (?,?,?,?,?,?,?,?,1)",
                ids.productId(), tenantId, "DEMO-LAMP-01", "演示产品｜智能桌灯", "铝合金外壳，USB-C，样品规格", "USD", 28.50, 100);
        jdbcTemplate.update("INSERT INTO inquiries (id,tenant_id,customer_id,subject,content,status,source_channel,external_id,owner_id,next_action_due) VALUES (?,?,?,?,?,?,?,?,?,?)",
                ids.inquiryId(), tenantId, ids.customerId(), "演示询盘｜1,200 件智能桌灯采购",
                "客户计划采购 1,200 件智能桌灯，要求提供 MOQ、FOB 报价、样品周期和预计交付时间。", "PROCESSING",
                "DEMO", ids.batchId(), userId, Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        jdbcTemplate.update("INSERT INTO followup_tasks (id,tenant_id,title,priority,status,due_at,related_type,related_id) VALUES (?,?,?,?,?,?,?,?)",
                ids.taskId(), tenantId, "演示任务｜确认报价与样品交期", "HIGH", "OPEN",
                Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)), "INQUIRY", ids.inquiryId());
        jdbcTemplate.update("INSERT INTO knowledge_articles (id,tenant_id,title,category,content,active_flag,updated_by) VALUES (?,?,?,?,?,?,?)",
                knowledgeId, tenantId, "演示产品与报价规则", "PRICING",
                "智能桌灯演示产品 MOQ 为 100 件，默认贸易条款为 FOB。正式报价前必须确认目的港、包装和交期。",
                true, userId);
        jdbcTemplate.update("INSERT INTO quotation_approval_rules (id,tenant_id,rule_name,rule_type,threshold_amount,condition_value,enabled_flag) VALUES (?,?,?,?,?,?,?)",
                approvalRuleId, tenantId, "演示报价金额审批", "AMOUNT_THRESHOLD", 10000, "", true);
        List<Map.Entry<String, String>> records = List.of(
                Map.entry("CUSTOMER", ids.customerId()), Map.entry("PRODUCT", ids.productId()),
                Map.entry("INQUIRY", ids.inquiryId()), Map.entry("TASK", ids.taskId()),
                Map.entry("DEPARTMENT", departmentId), Map.entry("KNOWLEDGE", knowledgeId),
                Map.entry("APPROVAL_RULE", approvalRuleId));
        records.forEach(record -> jdbcTemplate.update(
                "INSERT INTO tenant_demo_records (id,tenant_id,batch_id,entity_type,entity_id,created_by) VALUES (?,?,?,?,?,?)",
                "demo-record-" + java.util.UUID.randomUUID(), tenantId, ids.batchId(), record.getKey(), record.getValue(), userId));
    }

    @Override
    public int clearDemoData(String tenantId) {
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT entity_type,entity_id FROM tenant_demo_records WHERE tenant_id=?", tenantId);
        int removed = 0;
        for (Map<String, Object> record : records) {
            String table = switch (String.valueOf(record.get("entity_type"))) {
                case "TASK" -> "followup_tasks";
                case "INQUIRY" -> "inquiries";
                case "PRODUCT" -> "products";
                case "CUSTOMER" -> "customers";
                case "KNOWLEDGE" -> "knowledge_articles";
                case "APPROVAL_RULE" -> "quotation_approval_rules";
                case "DEPARTMENT" -> "departments";
                default -> "";
            };
            if (!table.isBlank()) {
                if ("DEPARTMENT".equals(record.get("entity_type"))) {
                    jdbcTemplate.update("UPDATE users SET department_id=NULL WHERE tenant_id=? AND department_id=?",
                            tenantId, String.valueOf(record.get("entity_id")));
                }
                removed += jdbcTemplate.update("DELETE FROM " + table + " WHERE tenant_id=? AND id=?",
                        tenantId, String.valueOf(record.get("entity_id")));
            }
        }
        jdbcTemplate.update("DELETE FROM tenant_demo_records WHERE tenant_id=?", tenantId);
        return removed;
    }

    private int count(String sql, String tenantId) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, tenantId);
        return value == null ? 0 : value;
    }
}
