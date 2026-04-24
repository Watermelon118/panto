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

    /**
     * 检查是否已有用户使用指定用户名。
     *
     * @param username 待校验的用户名
     * @return 如果已存在则返回 true
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * 检查除当前用户外，是否还有其他用户使用指定用户名。
     *
     * @param username 待校验的用户名
     * @param id 需要排除的用户 ID
     * @return 如果已存在则返回 true
     */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
