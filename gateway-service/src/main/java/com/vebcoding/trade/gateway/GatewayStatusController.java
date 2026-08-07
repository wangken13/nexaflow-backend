package com.vebcoding.trade.gateway;

import com.vebcoding.trade.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayStatusController {
    @GetMapping("/")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(Map.of(
                "service", "tradeflow-ai-gateway",
                "status", "UP",
                "routes", List.of(
                        "/api/auth",
                        "/api/tenant",
                        "/api/customer",
                        "/api/inquiry",
                        "/api/ai",
                        "/api/quotation",
                        "/api/order",
                        "/api/task",
                        "/api/notification",
                        "/api/file")));
    }
}
