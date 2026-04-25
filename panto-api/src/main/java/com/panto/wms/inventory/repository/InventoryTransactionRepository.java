package com.panto.wms.inventory.repository;

import com.panto.wms.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 库存事务数据访问接口。
 */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

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
          and (:transactionType is null or t.transactionType = :transactionType)
        order by t.createdAt desc
        """)
    Page<InventoryTransaction> search(
        @Param("productId") Long productId,
        @Param("transactionType") String transactionType,
        Pageable pageable
    );
}
