package com.vebcoding.trade.customer.service;

import com.vebcoding.trade.customer.api.CustomerDetailView;

public interface SensitiveDataMasker {
    CustomerDetailView mask(CustomerDetailView detail, String role);
}
