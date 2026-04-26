package com.panto.wms.inventory.service;

import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.settings.service.SystemSettingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 每日到期扫描服务，根据系统设置中的预警天数刷新批次的到期状态。
 */
@Slf4j
@Service
public class ExpiryScanService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Pacific/Auckland");

    private final BatchRepository batchRepository;
    private final SystemSettingService settingService;

    /**
     * 创建到期扫描服务。
     *
     * @param batchRepository 批次数据访问接口
     * @param settingService 系统设置服务
     */
    public ExpiryScanService(BatchRepository batchRepository, SystemSettingService settingService) {
        this.batchRepository = batchRepository;
        this.settingService = settingService;
    }

    /**
     * 每天 00:00 (Pacific/Auckland) 触发的定时扫描入口。
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Pacific/Auckland")
    public void runDailyScan() {
        log.info("Daily expiry scan starting");
        ScanResult result = runScan(LocalDate.now(BUSINESS_ZONE));
        log.info("Daily expiry scan completed: scanned={}, updated={}", result.scanned(), result.updated());
    }

    /**
     * 以指定日期为今天，重新计算所有活跃批次的到期状态。多次重跑得到相同结果。
     *
     * @param today 视为"今天"的日期
     * @return 扫描结果统计
     */
    @Transactional
    public ScanResult runScan(LocalDate today) {
        int threshold = settingService.getExpiryWarningDays();
        LocalDate warningEnd = today.plusDays(threshold);

        List<Batch> active = batchRepository.findAllActive();
        int updated = 0;
        for (Batch batch : active) {
            ExpiryStatus expected = classify(batch.getExpiryDate(), today, warningEnd);
            if (batch.getExpiryStatus() != expected) {
                batch.setExpiryStatus(expected);
                batchRepository.save(batch);
                updated++;
            }
        }
        return new ScanResult(active.size(), updated);
    }

    private ExpiryStatus classify(LocalDate expiryDate, LocalDate today, LocalDate warningEnd) {
        if (expiryDate.isBefore(today)) {
            return ExpiryStatus.EXPIRED;
        }
        if (!expiryDate.isAfter(warningEnd)) {
            return ExpiryStatus.EXPIRING_SOON;
        }
        return ExpiryStatus.NORMAL;
    }

    /**
     * 扫描结果统计。
     *
     * @param scanned 扫描的批次总数
     * @param updated 实际更新状态的批次数
     */
    public record ScanResult(int scanned, int updated) {
    }
}
