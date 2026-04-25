package com.panto.wms.inventory.repository;

import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.entity.Batch;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 库存批次数据访问接口。
 */
public interface BatchRepository extends JpaRepository<Batch, Long> {

    /**
     * 按 FIFO 顺序返回指定产品的可用批次（quantity_remaining > 0）。
     *
     * @param productId 产品 ID
     * @return 按到期日升序排列的可用批次
     */
    @Query("""
        select b from Batch b
        where b.productId = :productId
          and b.quantityRemaining > 0
        order by b.expiryDate asc
        """)
    List<Batch> findAvailableByProductIdOrderByExpiryDateAsc(@Param("productId") Long productId);

    /**
     * 统计指定产品在指定到货日期已存在的批次数，用于生成批次号序号。
     *
     * @param productId   产品 ID
     * @param arrivalDate 到货日期
     * @return 当天该产品批次总数
     */
    long countByProductIdAndArrivalDate(Long productId, LocalDate arrivalDate);

    /**
     * 返回指定入库明细 ID 集合对应的所有批次，用于更新入库单时校验库存是否被消耗。
     *
     * @param inboundItemIds 入库明细 ID 集合
     * @return 批次列表
     */
    List<Batch> findByInboundItemIdIn(Collection<Long> inboundItemIds);

    /**
     * 分页查询批次，支持产品和到期状态筛选。
     *
     * @param productId    产品 ID 筛选，可为空
     * @param expiryStatus 到期状态筛选，可为空
     * @param pageable     分页参数
     * @return 分页批次列表
     */
    @Query("""
        select b from Batch b
        where (:productId is null or b.productId = :productId)
          and (:#{#expiryStatus} is null or b.expiryStatus = :expiryStatus)
        order by b.expiryDate asc
        """)
    Page<Batch> search(
        @Param("productId") Long productId,
        @Param("expiryStatus") ExpiryStatus expiryStatus,
        Pageable pageable
    );
}
