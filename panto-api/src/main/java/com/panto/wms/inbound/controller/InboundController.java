package com.panto.wms.inbound.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.inbound.dto.CreateInboundRequest;
import com.panto.wms.inbound.dto.InboundDetailResponse;
import com.panto.wms.inbound.dto.InboundPageResponse;
import com.panto.wms.inbound.dto.UpdateInboundRequest;
import com.panto.wms.inbound.service.InboundService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入库单相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/inbound")
public class InboundController {

    private final InboundService inboundService;

    /**
     * 创建入库单控制器。
     *
     * @param inboundService 入库单业务服务
     */
    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    /**
     * 返回分页入库单列表，支持日期范围和产品筛选。
     *
     * @param dateFrom  起始日期，可为空
     * @param dateTo    结束日期，可为空
     * @param productId 产品 ID 筛选，可为空
     * @param page      页码
     * @param size      每页条数
     * @return 分页入库单列表
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<InboundPageResponse> listInbounds(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(required = false) Long productId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(inboundService.listInbounds(dateFrom, dateTo, productId, page, size));
    }

    /**
     * 返回入库单详情，含完整明细行。
     *
     * @param id 入库单 ID
     * @return 入库单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<InboundDetailResponse> getInbound(@PathVariable Long id) {
        return Result.success(inboundService.getInbound(id));
    }

    /**
     * 创建入库单，同时生成批次和库存事务（IN）。
     *
     * @param request           创建请求体
     * @param authenticatedUser 当前登录用户
     * @return 创建后的入库单详情
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public Result<InboundDetailResponse> createInbound(
        @Valid @RequestBody CreateInboundRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(inboundService.createInbound(request, authenticatedUser.getUserId()));
    }

    /**
     * 更新入库单。仅在该入库单所有批次库存未被消耗时允许修改。
     *
     * @param id                入库单 ID
     * @param request           更新请求体
     * @param authenticatedUser 当前登录用户
     * @return 更新后的入库单详情
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public Result<InboundDetailResponse> updateInbound(
        @PathVariable Long id,
        @Valid @RequestBody UpdateInboundRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(inboundService.updateInbound(id, request, authenticatedUser.getUserId()));
    }
}
