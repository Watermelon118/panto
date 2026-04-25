package com.panto.wms.order.repository;

import com.panto.wms.order.entity.Order;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 销售订单数据访问接口。
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 统计指定时间范围内创建的订单数量，用于生成订单号序列。
     *
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 时间范围内的订单数量
     */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime start, OffsetDateTime end);
}
