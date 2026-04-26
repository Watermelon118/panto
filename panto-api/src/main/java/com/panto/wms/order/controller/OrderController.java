package com.panto.wms.order.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.dto.CreateOrderRequest;
import com.panto.wms.order.dto.InvoiceResponse;
import com.panto.wms.order.dto.OrderPageResponse;
import com.panto.wms.order.dto.OrderResponse;
import com.panto.wms.order.dto.RollbackOrderRequest;
import com.panto.wms.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * 销售订单相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单控制器。
     *
     * @param orderService 订单业务服务
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 分页查询订单列表。
     *
     * @param customerId 客户 ID，可为空
     * @param dateFrom 起始日期，可为空
     * @param dateTo 结束日期，可为空
     * @param status 订单状态，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 订单分页结果
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public Result<OrderPageResponse> listOrders(
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(orderService.listOrders(customerId, dateFrom, dateTo, status, page, size));
    }

    /**
     * 查询订单详情。
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public Result<OrderResponse> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getOrder(id));
    }

    /**
     * 查询订单发票数据。
     *
     * @param id 订单 ID
     * @return 发票响应数据
     */
    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public Result<InvoiceResponse> getInvoice(@PathVariable Long id) {
        return Result.success(orderService.getInvoice(id));
    }

    /**
     * 下载订单发票 PDF。
     *
     * @param id 订单 ID
     * @return PDF 下载响应
     */
    @GetMapping("/{id}/invoice/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public ResponseEntity<ByteArrayResource> downloadInvoicePdf(@PathVariable Long id) {
        OrderService.InvoicePdfFile invoicePdfFile = orderService.getInvoicePdf(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(invoicePdfFile.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + invoicePdfFile.fileName() + "\"")
            .contentLength(invoicePdfFile.content().length)
            .body(new ByteArrayResource(invoicePdfFile.content()));
    }

    /**
     * 创建销售订单，并按 FIFO 规则扣减库存。
     *
     * @param request 创建请求体
     * @param authenticatedUser 当前登录用户
     * @return 创建后的订单详情
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public Result<OrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(orderService.createOrder(request, authenticatedUser.getUserId()));
    }

    /**
     * 回滚订单并返还库存。
     *
     * @param id 订单 ID
     * @param request 回滚请求体
     * @param authenticatedUser 当前登录用户
     * @return 回滚后的订单详情
     */
    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING')")
    public Result<OrderResponse> rollbackOrder(
        @PathVariable Long id,
        @Valid @RequestBody RollbackOrderRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return Result.success(orderService.rollbackOrder(id, request.reason(), authenticatedUser.getUserId()));
    }
}
