package com.vebcoding.trade.customer.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.customer.api.CreateCustomerRequest;
import com.vebcoding.trade.customer.api.CustomerView;
import com.vebcoding.trade.customer.api.ContactView;
import com.vebcoding.trade.customer.api.CreateContactRequest;
import com.vebcoding.trade.customer.api.CreateFollowupRequest;
import com.vebcoding.trade.customer.api.CustomerDetailView;
import com.vebcoding.trade.customer.api.FollowupView;
import com.vebcoding.trade.customer.api.CustomerBulkImportRequest;
import com.vebcoding.trade.common.BulkImportResult;
import com.vebcoding.trade.customer.api.AssignCustomerOwnerRequest;
import jakarta.validation.Valid;
import com.vebcoding.trade.customer.service.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<CustomerView>> list() {
        return ApiResponse.ok(customerService.list());
    }

    @PostMapping("/export")
    public ApiResponse<List<CustomerView>> exportData() {
        return ApiResponse.ok(customerService.exportData());
    }

    @PostMapping
    public ApiResponse<CustomerView> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDetailView> detail(@PathVariable String id) {
        return ApiResponse.ok(customerService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerView> update(@PathVariable String id,
                                             @Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @PatchMapping("/{id}/owner")
    public ApiResponse<CustomerView> assignOwner(@PathVariable String id,
                                                  @Valid @RequestBody AssignCustomerOwnerRequest request) {
        return ApiResponse.ok(customerService.assignOwner(id, request.ownerId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        customerService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/contacts")
    public ApiResponse<ContactView> addContact(@PathVariable String id,
                                                @Valid @RequestBody CreateContactRequest request) {
        return ApiResponse.ok(customerService.addContact(id, request));
    }

    @PostMapping("/{id}/followups")
    public ApiResponse<FollowupView> addFollowup(@PathVariable String id,
                                                  @Valid @RequestBody CreateFollowupRequest request) {
        return ApiResponse.ok(customerService.addFollowup(id, request));
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(customerService.tags());
    }

    @PostMapping("/import")
    public ApiResponse<BulkImportResult> bulkImport(@Valid @RequestBody CustomerBulkImportRequest request) {
        return ApiResponse.ok(customerService.bulkImport(request.rows()));
    }
}
