package com.vebcoding.trade.ai.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.ai.api.InquiryAnalysis;
import com.vebcoding.trade.ai.api.KnowledgeReference;
import com.vebcoding.trade.common.TenantContext;
import java.util.UUID;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiAnalysisMapper implements AiAnalysisMapper {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAiAnalysisMapper(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public InquiryAnalysis save(String inquiryId, InquiryAnalysis analysis) {
        jdbcTemplate.update("""
                INSERT INTO ai_analysis
                  (id, tenant_id, inquiry_id, intent, urgency, model_summary, next_actions_json, reply_draft,
                   quotation_draft, knowledge_sufficient, knowledge_sources_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "ana-" + UUID.randomUUID(),
                TenantContext.tenantId(),
                inquiryId == null ? "" : inquiryId,
                analysis.intent(),
                analysis.urgency(),
                analysis.modelSummary(),
                toJson(analysis),
                analysis.replyDraft(),
                analysis.quotationDraft(),
                analysis.knowledgeSufficient(),
                sourcesToJson(analysis.sources()));
        return analysis;
    }

    @Override
    public List<InquiryAnalysis> findByInquiryId(String tenantId, String inquiryId) {
        return jdbcTemplate.query("""
                SELECT intent, urgency, model_summary, next_actions_json, reply_draft, quotation_draft,
                  knowledge_sufficient, knowledge_sources_json
                FROM ai_analysis WHERE tenant_id=? AND inquiry_id=? ORDER BY created_at DESC
                """, (rs, rowNum) -> new InquiryAnalysis(rs.getString("intent"), rs.getString("urgency"),
                fromJson(rs.getString("next_actions_json")), rs.getString("model_summary"),
                rs.getString("reply_draft"), rs.getString("quotation_draft"),
                rs.getBoolean("knowledge_sufficient"), sourcesFromJson(rs.getString("knowledge_sources_json"))),
                tenantId, inquiryId);
    }

    private String toJson(InquiryAnalysis analysis) {
        try {
            return objectMapper.writeValueAsString(analysis.nextActions());
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String sourcesToJson(List<KnowledgeReference> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<KnowledgeReference> sourcesFromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, KnowledgeReference.class));
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
