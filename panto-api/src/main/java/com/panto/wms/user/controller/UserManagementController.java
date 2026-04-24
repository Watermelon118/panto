package com.panto.wms.user.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.user.dto.CreateUserRequest;
import com.panto.wms.user.dto.ResetUserPasswordRequest;
import com.panto.wms.user.dto.UpdateUserRequest;
import com.panto.wms.user.dto.UpdateUserStatusRequest;
import com.panto.wms.user.dto.UserPageResponse;
import com.panto.wms.user.dto.UserResponse;
import com.panto.wms.user.service.UserManagementService;
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
 * 用户管理相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * 创建用户管理控制器。
     *
     * @param userManagementService 用户管理业务服务
     */
    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * 返回分页用户列表。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserPageResponse> listUsers(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(userManagementService.listUsers(page, size));
    }

    /**
     * 返回用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> getUser(@PathVariable Long userId) {
        return Result.success(userManagementService.getUser(userId));
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求体
     * @param authenticatedUser 当前登录用户
     * @return 创建后的用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(userManagementService.createUser(request, authenticatedUser.getUserId()));
    }

    /**
     * 更新用户基础信息。
     *
     * @param userId 用户 ID
     * @param request 更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的用户
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(userManagementService.updateUser(userId, request, authenticatedUser.getUserId()));
    }

    /**
     * 更新用户启用状态。
     *
     * @param userId 用户 ID
     * @param request 状态更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的用户
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserStatusRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(
            userManagementService.updateUserStatus(userId, request.active(), authenticatedUser.getUserId())
        );
    }

    /**
     * 重置用户密码。
     *
     * @param userId 用户 ID
     * @param request 重置密码请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的用户
     */
    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserResponse> resetPassword(
        @PathVariable Long userId,
        @Valid @RequestBody ResetUserPasswordRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(
            userManagementService.resetPassword(userId, request, authenticatedUser.getUserId())
        );
    }
}
