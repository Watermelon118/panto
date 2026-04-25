package com.panto.wms.order.repository;

import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 分页查询订单，支持客户、时间范围和状态筛选。
     *
     * @param customerId 客户 ID，可为空
     * @param createdAtFrom 创建时间起点，可为空
     * @param createdAtTo 创建时间终点（不含），可为空
     * @param status 订单状态，可为空
     * @param pageable 分页参数
     * @return 订单分页结果
     */
    @Query("""
        select o from Order o
        where (:customerId is null or o.customerId = :customerId)
          and (:createdAtFrom is null or o.createdAt >= :createdAtFrom)
          and (:createdAtTo is null or o.createdAt < :createdAtTo)
          and (:#{#status} is null or o.status = :status)
        order by o.createdAt desc, o.id desc
        """)
    Page<Order> search(
        @Param("customerId") Long customerId,
        @Param("createdAtFrom") OffsetDateTime createdAtFrom,
        @Param("createdAtTo") OffsetDateTime createdAtTo,
        @Param("status") OrderStatus status,
        Pageable pageable
    );
}
