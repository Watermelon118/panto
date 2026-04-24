package com.panto.wms.auth.repository;

import com.panto.wms.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户数据访问接口。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);
}
