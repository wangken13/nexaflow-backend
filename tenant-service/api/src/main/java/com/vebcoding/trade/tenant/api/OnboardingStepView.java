package com.vebcoding.trade.tenant.api;

public record OnboardingStepView(String code, String title, String description, boolean completed, int currentCount) {
}
