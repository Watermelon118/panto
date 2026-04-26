package com.panto.wms.settings.repository;

import com.panto.wms.settings.entity.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统设置数据访问接口。
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /**
     * 按 setting_key 查询单条设置。
     *
     * @param settingKey 设置项 key
     * @return 设置项，可能为空
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);
}
