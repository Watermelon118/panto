package com.panto.wms.destruction.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.destruction.dto.CreateDestructionRequest;
import com.panto.wms.destruction.dto.DestructionPageResponse;
import com.panto.wms.destruction.dto.DestructionResponse;
import com.panto.wms.destruction.service.DestructionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销毁记录相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/destructions")
public class DestructionController {

    private final DestructionService destructionService;

    /**
     * 创建销毁记录控制器。
     *
     * @param destructionService 销毁业务服务
     */
    public DestructionController(DestructionService destructionService) {
        this.destructionService = destructionService;
    }

    /**
     * 分页查询销毁记录。
     *
     * @param productId 产品 ID，可为空
     * @param dateFrom 起始日期，可为空
     * @param dateTo 结束日期，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页销毁记录
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'ACCOUNTANT')")
    public Result<DestructionPageResponse> listDestructions(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(destructionService.listDestructions(productId, dateFrom, dateTo, page, size));
    }

    /**
     * 查询销毁记录详情。
     *
     * @param id 销毁记录 ID
     * @return 销毁详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'ACCOUNTANT')")
    public Result<DestructionResponse> getDestruction(@PathVariable Long id) {
        return Result.success(destructionService.getDestruction(id));
    }

    /**
     * 创建销毁记录。
     *
     * @param request 创建请求体
     * @param authenticatedUser 当前登录用户
     * @return 创建后的销毁详情
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public Result<DestructionResponse> createDestruction(
        @Valid @RequestBody CreateDestructionRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(destructionService.createDestruction(request, authenticatedUser.getUserId()));
    }
}
