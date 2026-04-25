package com.panto.wms.customer.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.customer.dto.CreateCustomerRequest;
import com.panto.wms.customer.dto.CustomerPageResponse;
import com.panto.wms.customer.dto.CustomerResponse;
import com.panto.wms.customer.dto.UpdateCustomerRequest;
import com.panto.wms.customer.dto.UpdateCustomerStatusRequest;
import com.panto.wms.customer.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * 客户管理相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * 创建客户控制器。
     *
     * @param customerService 客户业务服务
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 返回分页筛选后的客户列表。
     *
     * @param keyword 公司名称或电话关键字，可为空
     * @param active 启用状态，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页客户列表
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public Result<CustomerPageResponse> listCustomers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(customerService.listCustomers(keyword, active, page, size));
    }

    /**
     * 返回客户详情。
     *
     * @param customerId 客户 ID
     * @return 客户详情
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    public Result<CustomerResponse> getCustomer(@PathVariable Long customerId) {
        return Result.success(customerService.getCustomer(customerId));
    }

    /**
     * 创建客户。
     *
     * @param request 创建请求体
     * @param authenticatedUser 当前登录用户
     * @return 创建后的客户
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    public Result<CustomerResponse> createCustomer(
        @Valid @RequestBody CreateCustomerRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(customerService.createCustomer(request, authenticatedUser.getUserId()));
    }

    /**
     * 更新客户。
     *
     * @param customerId 客户 ID
     * @param request 更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的客户
     */
    @PutMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    public Result<CustomerResponse> updateCustomer(
        @PathVariable Long customerId,
        @Valid @RequestBody UpdateCustomerRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(customerService.updateCustomer(customerId, request, authenticatedUser.getUserId()));
    }

    /**
     * 更新客户启用状态。
     *
     * @param customerId 客户 ID
     * @param request 状态更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的客户
     */
    @PatchMapping("/{customerId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING')")
    public Result<CustomerResponse> updateCustomerStatus(
        @PathVariable Long customerId,
        @Valid @RequestBody UpdateCustomerStatusRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(
            customerService.updateCustomerStatus(customerId, request.active(), authenticatedUser.getUserId())
        );
    }
}
