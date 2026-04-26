package com.panto.wms.order.repository;

import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
     * 分页查询某个客户的订单历史。
     *
     * @param customerId 客户 ID
     * @param pageable 分页参数
     * @return 订单分页结果
     */
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

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
    @Query(
        value = """
            select *
            from orders o
            where (cast(:customerId as bigint) is null or o.customer_id = cast(:customerId as bigint))
              and (
                    cast(:createdAtFrom as timestamptz) is null
                    or o.created_at >= cast(:createdAtFrom as timestamptz)
                  )
              and (
                    cast(:createdAtTo as timestamptz) is null
                    or o.created_at < cast(:createdAtTo as timestamptz)
                  )
              and (cast(:status as varchar) is null or o.status = cast(:status as varchar))
            order by o.created_at desc, o.id desc
            """,
        countQuery = """
            select count(*)
            from orders o
            where (cast(:customerId as bigint) is null or o.customer_id = cast(:customerId as bigint))
              and (
                    cast(:createdAtFrom as timestamptz) is null
                    or o.created_at >= cast(:createdAtFrom as timestamptz)
                  )
              and (
                    cast(:createdAtTo as timestamptz) is null
                    or o.created_at < cast(:createdAtTo as timestamptz)
                  )
              and (cast(:status as varchar) is null or o.status = cast(:status as varchar))
            """,
        nativeQuery = true
    )
    Page<Order> search(
        @Param("customerId") Long customerId,
        @Param("createdAtFrom") OffsetDateTime createdAtFrom,
        @Param("createdAtTo") OffsetDateTime createdAtTo,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * 查询指定状态和时间范围内的订单列表。
     *
     * @param status 订单状态
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 订单列表
     */
    List<Order> findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        OrderStatus status,
        OffsetDateTime start,
        OffsetDateTime end
    );

    /**
     * 汇总指定状态和时间范围内订单的销售总额。
     *
     * @param status 订单状态
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 销售总额
     */
    @Query("""
        select coalesce(sum(o.totalAmount), 0)
        from Order o
        where o.status = :status
          and o.createdAt >= :start
          and o.createdAt < :end
        """)
    BigDecimal sumTotalAmountByStatusAndCreatedAtRange(
        @Param("status") OrderStatus status,
        @Param("start") OffsetDateTime start,
        @Param("end") OffsetDateTime end
    );

    /**
     * 汇总某个客户在指定状态下的订单总金额。
     *
     * @param customerId 客户 ID
     * @param status 订单状态
     * @return 订单总金额
     */
    @Query("""
        select coalesce(sum(o.totalAmount), 0)
        from Order o
        where o.customerId = :customerId
          and o.status = :status
        """)
    BigDecimal sumTotalAmountByCustomerIdAndStatus(
        @Param("customerId") Long customerId,
        @Param("status") OrderStatus status
    );
}
