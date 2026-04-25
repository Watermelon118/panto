package com.panto.wms.product.repository;

import com.panto.wms.product.entity.Product;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 产品数据访问接口。
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 检查是否已有产品使用指定 SKU。
     *
     * @param sku 待校验的 SKU
     * @return 如果已存在则返回 true
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * 检查除当前产品外，是否还有其他产品使用指定 SKU。
     *
     * @param sku 待校验的 SKU
     * @param id 需要排除的产品 ID
     * @return 如果已存在则返回 true
     */
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    /**
     * 返回分页筛选后的产品列表。
     *
     * @param keyword SKU 或名称关键字，可为空
     * @param category 分类筛选，可为空
     * @param active 启用状态筛选，可为空
     * @param pageable 分页参数
     * @return 分页产品列表
     */
    @Query("""
        select p
        from Product p
        where (:keyword is null
            or lower(p.sku) like lower(concat('%', :keyword, '%'))
            or lower(p.name) like lower(concat('%', :keyword, '%')))
          and (:category is null or lower(p.category) = lower(:category))
          and (:active is null or p.active = :active)
        """)
    Page<Product> search(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("active") Boolean active,
        Pageable pageable
    );

    /**
     * 返回启用产品的分类列表，用于下拉框。
     *
     * @return 去重后的分类列表
     */
    @Query("""
        select distinct p.category
        from Product p
        where p.active = true
        order by p.category asc
        """)
    List<String> findDistinctActiveCategories();

    /**
     * 返回启用产品的单位列表，用于下拉框。
     *
     * @return 去重后的单位列表
     */
    @Query("""
        select distinct p.unit
        from Product p
        where p.active = true
        order by p.unit asc
        """)
    List<String> findDistinctActiveUnits();

    /**
     * 返回所有启用产品，用于低库存计算。
     *
     * @return 启用产品列表
     */
    List<Product> findByActiveTrue();

    /**
     * 返回指定产品 ID 列表的聚合库存。
     *
     * @param productIds 产品 ID 列表
     * @return 聚合库存结果
     */
    @Query(value = """
        select p.id as productId, coalesce(sum(b.quantity_remaining), 0) as currentStock
        from products p
        left join batches b on b.product_id = p.id
        where p.id in (:productIds)
        group by p.id
        """, nativeQuery = true)
    List<ProductStockView> findCurrentStockByProductIds(@Param("productIds") Collection<Long> productIds);
}
