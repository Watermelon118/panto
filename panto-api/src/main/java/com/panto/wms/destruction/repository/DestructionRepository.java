package com.panto.wms.destruction.repository;

import com.panto.wms.destruction.entity.Destruction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 销毁记录数据访问接口。
 */
public interface DestructionRepository extends JpaRepository<Destruction, Long> {

    /**
     * 统计指定时间范围内创建的销毁记录数量，用于生成销毁单号序列。
     *
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 范围内的销毁记录数量
     */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime start, OffsetDateTime end);

    /**
     * 分页查询销毁记录，支持按产品和日期范围筛选。
     *
     * @param productId 产品 ID，可为空
     * @param createdAtFrom 创建时间起点，可为空
     * @param createdAtTo 创建时间终点（不含），可为空
     * @param pageable 分页参数
     * @return 分页销毁记录
     */
    @Query("""
        select d from Destruction d
        where (:productId is null or d.productId = :productId)
          and (:createdAtFrom is null or d.createdAt >= :createdAtFrom)
          and (:createdAtTo is null or d.createdAt < :createdAtTo)
        order by d.createdAt desc, d.id desc
        """)
    Page<Destruction> findByFilters(
        @Param("productId") Long productId,
        @Param("createdAtFrom") OffsetDateTime createdAtFrom,
        @Param("createdAtTo") OffsetDateTime createdAtTo,
        Pageable pageable
    );

    /**
     * 统计指定时间范围内的销毁损耗总额。
     *
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 损耗总额
     */
    @Query(
        value = """
            select coalesce(sum(d.loss_amount), 0)
            from destructions d
            where d.created_at >= :start
              and d.created_at < :end
            """,
        nativeQuery = true
    )
    BigDecimal sumLossInRange(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
