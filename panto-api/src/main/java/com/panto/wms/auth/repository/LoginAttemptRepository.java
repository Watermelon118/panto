package com.panto.wms.auth.repository;

import com.panto.wms.auth.entity.LoginAttempt;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 登录尝试数据访问接口。
 */
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * 删除创建时间早于给定时刻的登录尝试记录。
     *
     * @param cutoff 截止时间（含 cutoff 之前）
     * @return 实际删除行数
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
