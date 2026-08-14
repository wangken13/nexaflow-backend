package com.vebcoding.trade.product.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.product.api.ProductView;
import com.vebcoding.trade.product.api.UpsertProductRequest;
import com.vebcoding.trade.product.api.ProductBulkImportRequest;
import com.vebcoding.trade.common.BulkImportResult;
import jakarta.validation.Valid;
import com.vebcoding.trade.product.service.ProductService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @GetMapping public ApiResponse<List<ProductView>> list(@RequestParam(defaultValue = "") String keyword) { return ApiResponse.ok(service.list(keyword)); }
    @GetMapping("/{id}") public ApiResponse<ProductView> get(@PathVariable String id) { return ApiResponse.ok(service.get(id)); }
    @PostMapping public ApiResponse<ProductView> create(@RequestBody UpsertProductRequest request) { return ApiResponse.ok(service.create(request)); }
    @PutMapping("/{id}") public ApiResponse<ProductView> update(@PathVariable String id, @RequestBody UpsertProductRequest request) { return ApiResponse.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable String id) { service.delete(id); return ApiResponse.ok(null); }
    @PostMapping("/import") public ApiResponse<BulkImportResult> bulkImport(@Valid @RequestBody ProductBulkImportRequest request) { return ApiResponse.ok(service.bulkImport(request.rows())); }
}
