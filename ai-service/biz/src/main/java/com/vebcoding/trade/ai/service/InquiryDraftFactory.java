package com.vebcoding.trade.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class InquiryDraftFactory {
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+[,.]?\\d*)\\s*(pcs|pieces|sets|units|个|件|套)", Pattern.CASE_INSENSITIVE);

    public List<String> nextActions(String content, String intent) {
        if ("非外贸业务咨询".equals(intent)) {
            return List.of("确认客户真实需求", "判断是否属于可服务范围", "提供匹配方案或礼貌转接");
        }

        List<String> actions = new ArrayList<>();
        if (extractQuantity(content).isEmpty()) {
            actions.add("确认采购数量");
        }
        if (!containsAny(content, "spec", "规格", "尺寸", "材质", "color", "颜色")) {
            actions.add("确认产品规格");
        }
        if (!containsAny(content, "delivery", "lead time", "交期", "发货", "到货")) {
            actions.add("补充目标交期");
        }
        if (!containsAny(content, "port", "destination", "目的港", "地址", "country", "国家")) {
            actions.add("确认目的港或收货国家");
        }
        actions.add("生成匹配客户需求的回复草稿");
        return actions;
    }

    public String replyDraft(String content, String intent) {
        if ("非外贸业务咨询".equals(intent)) {
            return "您好，感谢您的咨询。我们已收到您的需求。为便于匹配合适方案，请补充您的目标、当前基础、预算范围和期望交付时间，我们会根据实际情况提供建议。";
        }

        String quantity = extractQuantity(content);
        String quantityText = quantity.isEmpty() ? "采购数量" : quantity;
        return "您好，感谢您的询盘。我们已了解您关于" + quantityText
                + "的需求。为了给您提供准确报价，请确认产品规格、包装要求、目的港和目标交期。信息确认后，我们会尽快提供正式报价和交付方案。";
    }

    public String quotationDraft(String content, String intent) {
        if ("非外贸业务咨询".equals(intent)) {
            return "当前内容不属于标准外贸产品报价场景，暂不生成 FOB/CIF 报价。建议先确认客户所需服务范围、交付物和预算。";
        }

        String quantity = extractQuantity(content);
        String quantityText = quantity.isEmpty() ? "待确认数量" : quantity;
        return "报价草案：产品和规格待确认，数量 " + quantityText
                + "。建议按 EXW/FOB/CIF 三种贸易条款分别核价，报价有效期 7 天，最终价格以规格、包装、目的港和实时运费为准。";
    }

    public String requirementSummary(String content, String modelText, String intent, String urgency) {
        String cleanModelText = stripMarkdown(modelText);
        if (!cleanModelText.isBlank() && !cleanModelText.startsWith("未配置模型 Key")) {
            return cleanModelText;
        }
        if ("非外贸业务咨询".equals(intent)) {
            return "客户当前需求偏向咨询或服务类问题，不适合直接生成外贸产品报价。应先确认服务范围、目标结果、预算和时间要求。";
        }
        String quantity = extractQuantity(content);
        String quantityText = quantity.isEmpty() ? "未明确数量" : "数量为 " + quantity;
        return "客户意图为" + intent + "，紧急程度为" + urgency + "。" + quantityText
                + "，仍需补齐规格、包装、目的港和交期后再生成正式报价。";
    }

    private String extractQuantity(String content) {
        Matcher matcher = QUANTITY_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private boolean containsAny(String content, String... keywords) {
        String lower = content.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String stripMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("**", "")
                .replace("__", "")
                .replaceAll("(?m)^\\s*[-*]\\s+", "")
                .trim();
    }
}
