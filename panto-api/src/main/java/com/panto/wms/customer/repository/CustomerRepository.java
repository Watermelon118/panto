package com.panto.wms.customer.repository;

import com.panto.wms.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 客户数据访问接口。
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * 返回分页筛选后的客户列表。
     *
     * @param keyword 公司名称或电话关键字，可为空
     * @param active 启用状态筛选，可为空
     * @param pageable 分页参数
     * @return 分页客户列表
     */
    @Query("""
        select c
        from Customer c
        where (:keyword is null
            or lower(c.companyName) like lower(concat('%', :keyword, '%'))
            or lower(c.phone) like lower(concat('%', :keyword, '%')))
          and (:active is null or c.active = :active)
        """)
    Page<Customer> search(
        @Param("keyword") String keyword,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
