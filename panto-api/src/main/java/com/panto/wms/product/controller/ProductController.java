package com.panto.wms.product.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.product.dto.CreateProductRequest;
import com.panto.wms.product.dto.ProductPageResponse;
import com.panto.wms.product.dto.ProductResponse;
import com.panto.wms.product.dto.UpdateProductRequest;
import com.panto.wms.product.dto.UpdateProductStatusRequest;
import com.panto.wms.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品管理相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 创建产品控制器。
     *
     * @param productService 产品业务服务
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 返回分页筛选后的产品列表。
     *
     * @param keyword 关键字，可为空
     * @param category 分类，可为空
     * @param active 启用状态，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页产品列表
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<ProductPageResponse> listProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(productService.listProducts(keyword, category, active, page, size));
    }

    /**
     * 返回单个产品详情及当前聚合库存。
     *
     * @param productId 产品 ID
     * @return 产品详情
     */
    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<ProductResponse> getProduct(@PathVariable Long productId) {
        return Result.success(productService.getProduct(productId));
    }

    /**
     * 创建产品。
     *
     * @param request 创建请求体
     * @param authenticatedUser 当前登录用户
     * @return 创建后的产品
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public Result<ProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(productService.createProduct(request, authenticatedUser.getUserId()));
    }

    /**
     * 更新产品。
     *
     * @param productId 产品 ID
     * @param request 更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的产品
     */
    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public Result<ProductResponse> updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody UpdateProductRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(productService.updateProduct(productId, request, authenticatedUser.getUserId()));
    }

    /**
     * 更新产品启用状态。
     *
     * @param productId 产品 ID
     * @param request 状态更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的产品
     */
    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public Result<ProductResponse> updateProductStatus(
        @PathVariable Long productId,
        @Valid @RequestBody UpdateProductStatusRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(
            productService.updateProductStatus(productId, request.active(), authenticatedUser.getUserId())
        );
    }

    /**
     * 返回启用产品的分类列表，供下拉框使用。
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<List<String>> listCategories() {
        return Result.success(productService.listCategories());
    }

    /**
     * 返回启用产品的单位列表，供下拉框使用。
     *
     * @return 单位列表
     */
    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<List<String>> listUnits() {
        return Result.success(productService.listUnits());
    }
}
