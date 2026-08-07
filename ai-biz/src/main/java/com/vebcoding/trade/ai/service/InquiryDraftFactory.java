package com.vebcoding.trade.ai.service;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InquiryDraftFactory {
    public List<String> nextActions() {
        return List.of("确认产品规格", "补充 MOQ 和交期", "生成报价草稿");
    }

    public String replyDraft() {
        return "您好，感谢您的询盘。我们可以根据您的数量和目标交期提供报价，请确认规格、包装和目的港。";
    }

    public String quotationDraft() {
        return "建议报价：FOB Shanghai，单价按数量阶梯计算，有效期 7 天。";
    }
}
