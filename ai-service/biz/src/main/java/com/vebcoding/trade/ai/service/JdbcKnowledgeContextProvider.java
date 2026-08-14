package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.common.TenantContext;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcKnowledgeContextProvider implements KnowledgeContextProvider {
    private final JdbcTemplate jdbcTemplate;

    public JdbcKnowledgeContextProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String contextFor(String inquiryContent) {
        List<String> entries = jdbcTemplate.query("""
                SELECT title, category, LEFT(content, 2000) content
                FROM knowledge_articles
                WHERE tenant_id=? AND active_flag=1
                ORDER BY CASE WHEN ? LIKE CONCAT('%', title, '%') THEN 0 ELSE 1 END, updated_at DESC
                LIMIT 5
                """, (rs, rowNum) -> "[" + rs.getString("category") + "] " + rs.getString("title")
                + "\n" + rs.getString("content"), TenantContext.tenantId(), inquiryContent);
        return String.join("\n\n", entries);
    }
}
