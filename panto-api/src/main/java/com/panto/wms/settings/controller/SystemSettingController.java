package com.panto.wms.settings.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.settings.dto.SystemSettingsResponse;
import com.panto.wms.settings.dto.UpdateSystemSettingsRequest;
import com.panto.wms.settings.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/settings")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * 创建系统设置控制器。
     *
     * @param systemSettingService 系统设置业务服务
     */
    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    /**
     * 返回当前系统设置。
     *
     * @return 系统设置
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SystemSettingsResponse> getSettings() {
        return Result.success(new SystemSettingsResponse(systemSettingService.getExpiryWarningDays()));
    }

    /**
     * 更新系统设置。
     *
     * @param request 更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的系统设置
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SystemSettingsResponse> updateSettings(
        @Valid @RequestBody UpdateSystemSettingsRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        systemSettingService.update(
            SystemSettingService.KEY_EXPIRY_WARNING_DAYS,
            String.valueOf(request.expiryWarningDays()),
            authenticatedUser.getUserId()
        );
        return Result.success(new SystemSettingsResponse(systemSettingService.getExpiryWarningDays()));
    }
}
