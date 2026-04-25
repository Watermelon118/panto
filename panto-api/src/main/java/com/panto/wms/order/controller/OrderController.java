package com.panto.wms.order.controller;

import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.common.api.Result;
import com.panto.wms.order.dto.CreateOrderRequest;
import com.panto.wms.order.dto.OrderResponse;
import com.panto.wms.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销售订单相关 REST 接口控制器。
 */
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
}
