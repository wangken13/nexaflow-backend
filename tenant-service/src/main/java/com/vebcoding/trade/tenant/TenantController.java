package com.vebcoding.trade.tenant;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        return ApiResponse.ok(Map.of(
                "tenantId", TenantContext.tenantId(),
                "name", "Demo 外贸团队",
                "plan", "PRO",
                "aiCreditsUsed", 128,
                "aiCreditsLimit", 3000));
    }
}
