package com.panto.wms.auth.service;

import com.panto.wms.auth.repository.LoginAttemptRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录尝试日志的定期清理服务，按设计文档要求保留最近 90 天的数据。
 */
@Slf4j
@Service
public class LoginAttemptCleanupService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Pacific/Auckland");
    private static final int RETENTION_DAYS = 90;

    private final LoginAttemptRepository loginAttemptRepository;

    /**
     * 创建登录尝试清理服务。
     *
     * @param loginAttemptRepository 登录尝试数据访问接口
     */
    public LoginAttemptCleanupService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    /**
     * 每天 02:00 (Pacific/Auckland) 删除 90 天前的登录尝试记录。
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Pacific/Auckland")
    public void runDailyCleanup() {
        OffsetDateTime cutoff = OffsetDateTime.now(BUSINESS_ZONE).minusDays(RETENTION_DAYS);
        int deleted = cleanup(cutoff);
        log.info("Login attempt cleanup completed: cutoff={}, deleted={}", cutoff, deleted);
    }

    /**
     * 删除 cutoff 之前的所有登录尝试记录，返回实际删除行数。可被测试或手工触发。
     *
     * @param cutoff 截止时间（含 cutoff 之前的行被删除）
     * @return 删除的行数
     */
    @Transactional
    public int cleanup(OffsetDateTime cutoff) {
        return loginAttemptRepository.deleteOlderThan(cutoff);
    }
}
