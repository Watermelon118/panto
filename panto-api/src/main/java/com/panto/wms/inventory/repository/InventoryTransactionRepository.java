package com.panto.wms.inventory.repository;

import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.entity.InventoryTransaction;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 库存事务数据访问接口。
 */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    /**
     * 批量删除指定批次 ID 的库存事务，仅在入库单更新（无库存消耗）时调用。
     *
     * @param batchIds 批次 ID 集合
     */
    @Modifying
    @Query("delete from InventoryTransaction t where t.batchId in :batchIds")
    void deleteByBatchIdIn(@Param("batchIds") Collection<Long> batchIds);

    /**
     * 分页查询库存事务，支持产品和事务类型筛选。
     *
     * @param productId       产品 ID 筛选，可为空
     * @param transactionType 事务类型筛选，可为空
     * @param pageable        分页参数
     * @return 分页库存事务列表
     */
    @Query("""
        select t from InventoryTransaction t
        where (:productId is null or t.productId = :productId)
          and (:#{#transactionType} is null or t.transactionType = :transactionType)
        order by t.createdAt desc
        """)
    Page<InventoryTransaction> search(
        @Param("productId") Long productId,
        @Param("transactionType") TransactionType transactionType,
        Pageable pageable
    );
}
