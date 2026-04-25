package com.panto.wms.order.repository;

import com.panto.wms.order.entity.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 订单明细数据访问接口。
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 按订单 ID 查询明细，并按主键升序返回。
     *
     * @param orderId 订单 ID
     * @return 订单明细列表
     */
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    /**
     * 查询多个订单的全部明细。
     *
     * @param orderIds 订单 ID 集合
     * @return 订单明细列表
     */
    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);
}
