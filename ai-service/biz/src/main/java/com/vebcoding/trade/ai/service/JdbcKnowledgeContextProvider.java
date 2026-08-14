package com.vebcoding.trade.ai.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.ai.api.KnowledgeReference;
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
    public KnowledgeContext contextFor(String inquiryContent) {
        List<KnowledgeEntry> entries = jdbcTemplate.query("""
                SELECT id, title, category, LEFT(content, 2000) content
                FROM knowledge_articles
                WHERE tenant_id=? AND active_flag=1
                ORDER BY CASE WHEN ? LIKE CONCAT('%', title, '%') THEN 0 ELSE 1 END, updated_at DESC
                LIMIT 5
                """, (rs, rowNum) -> new KnowledgeEntry(rs.getString("id"), rs.getString("title"),
                rs.getString("category"), rs.getString("content")), TenantContext.tenantId(), inquiryContent);
        String prompt = entries.stream().map(entry -> "[KB:" + entry.id() + "][" + entry.category() + "] "
                + entry.title() + "\n" + entry.content()).collect(java.util.stream.Collectors.joining("\n\n"));
        List<KnowledgeReference> references = entries.stream()
                .map(entry -> new KnowledgeReference(entry.id(), entry.title(), entry.category())).toList();
        return new KnowledgeContext(prompt, references);
    }

    private record KnowledgeEntry(String id, String title, String category, String content) {
    }
}
