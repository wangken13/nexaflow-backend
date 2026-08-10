package com.vebcoding.trade.customer.api;

import java.util.List;

public record CustomerDetailView(CustomerView customer, List<ContactView> contacts, List<FollowupView> timeline) {
}
