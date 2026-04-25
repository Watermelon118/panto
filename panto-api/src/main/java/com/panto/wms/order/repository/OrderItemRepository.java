package com.panto.wms.order.repository;

import com.panto.wms.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 订单明细数据访问接口。
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
