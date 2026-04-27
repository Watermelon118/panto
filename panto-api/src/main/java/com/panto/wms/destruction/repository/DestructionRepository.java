package com.panto.wms.destruction.repository;

import com.panto.wms.destruction.entity.Destruction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 销毁记录数据访问接口。
 */
public interface DestructionRepository
    extends JpaRepository<Destruction, Long>, JpaSpecificationExecutor<Destruction> {

    /**
     * 统计指定时间范围内创建的销毁记录数量，用于生成销毁单号序列。
     *
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 范围内的销毁记录数量
     */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime start, OffsetDateTime end);

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

    /**
     * 查询指定时间范围内的销毁记录，按创建时间和主键升序返回。
     *
     * @param start 起始时间（含）
     * @param end 结束时间（不含）
     * @return 销毁记录列表
     */
    List<Destruction> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
        OffsetDateTime start,
        OffsetDateTime end
    );
}
