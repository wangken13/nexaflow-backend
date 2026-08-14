package com.vebcoding.trade.tenant.api;

import java.util.List;

public record OnboardingView(int completedSteps, int totalSteps, boolean demoDataPresent,
                             List<OnboardingStepView> steps) {
}
