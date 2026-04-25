package com.panto.wms.inbound.repository;

import com.panto.wms.inbound.entity.InboundRecord;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 入库单数据访问接口。
 */
public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {

    /**
     * 检查单号是否已存在。
     *
     * @param inboundNumber 入库单号
     * @return 如果已存在则返回 true
     */
    boolean existsByInboundNumber(String inboundNumber);

    /**
     * 统计指定日期已存在的入库单数量，用于生成序号。
     *
     * @param date 入库日期
     * @return 当天入库单总数
     */
    long countByInboundDate(LocalDate date);

    /**
     * 分页查询入库记录，支持日期范围和产品筛选。
     *
     * @param dateFrom  起始日期，可为空
     * @param dateTo    结束日期，可为空
     * @param productId 产品 ID 筛选，可为空
     * @param pageable  分页参数
     * @return 分页入库记录列表
     */
    @Query("""
        select r from InboundRecord r
        where (:dateFrom is null or r.inboundDate >= :dateFrom)
          and (:dateTo is null or r.inboundDate <= :dateTo)
          and (:productId is null or r.id in (
                select i.inboundRecordId from InboundItem i where i.productId = :productId
              ))
        order by r.inboundDate desc, r.id desc
        """)
    Page<InboundRecord> search(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        @Param("productId") Long productId,
        Pageable pageable
    );
}
