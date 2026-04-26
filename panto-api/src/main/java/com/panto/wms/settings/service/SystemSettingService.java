package com.panto.wms.settings.service;

import com.panto.wms.audit.annotation.Auditable;
import com.panto.wms.audit.domain.AuditAction;
import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.settings.entity.SystemSetting;
import com.panto.wms.settings.repository.SystemSettingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置业务服务，提供读写与轻量内存缓存。
 */
@Slf4j
@Service
public class SystemSettingService {

    /** 过期预警天数的设置 key。*/
    public static final String KEY_EXPIRY_WARNING_DAYS = "expiry_warning_days";

    /** 过期预警天数缺失或解析失败时使用的兜底值。*/
    public static final int DEFAULT_EXPIRY_WARNING_DAYS = 30;

    private static final int EXPIRY_WARNING_DAYS_MAX = 3650;

    private final SystemSettingRepository repository;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 创建系统设置服务。
     *
     * @param repository 系统设置数据访问接口
     */
    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    /**
     * 列出全部系统设置，按 key 升序。
     *
     * @return 设置列表
     */
    @Transactional(readOnly = true)
    public List<SystemSetting> listAll() {
        return repository.findAll().stream()
            .sorted((a, b) -> a.getSettingKey().compareTo(b.getSettingKey()))
            .toList();
    }

    /**
     * 按 key 读取设置值，优先走内存缓存。
     *
     * @param key 设置项 key
     * @return 设置值，不存在时返回空
     */
    @Transactional(readOnly = true)
    public Optional<String> getValue(String key) {
        String cached = cache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        return repository.findBySettingKey(key)
            .map(setting -> {
                cache.put(setting.getSettingKey(), setting.getSettingValue());
                return setting.getSettingValue();
            });
    }

    /**
     * 读取过期预警天数。缺失或解析失败时返回兜底值。
     *
     * @return 过期预警天数
     */
    @Transactional(readOnly = true)
    public int getExpiryWarningDays() {
        return getValue(KEY_EXPIRY_WARNING_DAYS)
            .map(value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    log.warn("expiry_warning_days '{}' is not a valid integer, falling back to default", value);
                    return DEFAULT_EXPIRY_WARNING_DAYS;
                }
            })
            .orElse(DEFAULT_EXPIRY_WARNING_DAYS);
    }

    /**
     * 写入或更新一条设置，并刷新缓存。
     *
     * @param key 设置项 key
     * @param value 设置项值
     * @param operatorId 操作人 ID
     * @return 更新后的设置
     */
    @Transactional
    @Auditable(
        action = AuditAction.UPDATE,
        entityType = "SYSTEM_SETTING",
        entityClass = SystemSetting.class,
        entityId = "#result.id",
        description = "更新系统设置"
    )
    public SystemSetting update(String key, String value, Long operatorId) {
        String normalizedKey = normalizeKey(key);
        String normalizedValue = normalizeValue(value);
        validateValue(normalizedKey, normalizedValue);

        SystemSetting setting = repository.findBySettingKey(normalizedKey)
            .orElseGet(() -> {
                SystemSetting created = new SystemSetting();
                created.setSettingKey(normalizedKey);
                return created;
            });

        setting.setSettingValue(normalizedValue);
        setting.setUpdatedAt(OffsetDateTime.now());
        setting.setUpdatedBy(operatorId);

        SystemSetting saved = repository.save(setting);
        cache.put(saved.getSettingKey(), saved.getSettingValue());
        return saved;
    }

    /**
     * 清空内存缓存，主要供测试使用。
     */
    public void clearCache() {
        cache.clear();
    }

    private void validateValue(String key, String value) {
        if (KEY_EXPIRY_WARNING_DAYS.equals(key)) {
            int days;
            try {
                days = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "expiry_warning_days must be an integer"
                );
            }
            if (days < 0 || days > EXPIRY_WARNING_DAYS_MAX) {
                throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "expiry_warning_days must be between 0 and " + EXPIRY_WARNING_DAYS_MAX
                );
            }
        }
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "settingKey must not be blank");
        }
        return key.trim();
    }

    private String normalizeValue(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "settingValue must not be null");
        }
        return value.trim();
    }
}
