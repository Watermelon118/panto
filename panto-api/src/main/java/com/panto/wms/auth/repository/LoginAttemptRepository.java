package com.panto.wms.auth.repository;

import com.panto.wms.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 登录尝试数据访问接口。
 */
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
}
