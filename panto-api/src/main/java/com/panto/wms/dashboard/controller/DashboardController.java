package com.panto.wms.dashboard.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.dashboard.dto.DashboardSummaryResponse;
import com.panto.wms.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板相关 REST 接口控制器。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 创建看板控制器。
     *
     * @param dashboardService 看板业务服务
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取当前用户角色对应的看板摘要。
     *
     * @param authenticatedUser 当前登录用户
     * @return 看板摘要
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<DashboardSummaryResponse> getSummary(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(dashboardService.getSummary(authenticatedUser.getRole()));
    }
}
