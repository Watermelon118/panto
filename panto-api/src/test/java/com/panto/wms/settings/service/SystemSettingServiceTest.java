package com.panto.wms.settings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.settings.entity.SystemSetting;
import com.panto.wms.settings.repository.SystemSettingRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 系统设置服务测试。
 */
@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository repository;

    @InjectMocks
    private SystemSettingService service;

    @Captor
    private ArgumentCaptor<SystemSetting> settingCaptor;

    @Test
    void getValueShouldLoadFromRepositoryOnFirstCallAndCacheAfter() {
        SystemSetting setting = buildSetting("expiry_warning_days", "30");
        when(repository.findBySettingKey("expiry_warning_days")).thenReturn(Optional.of(setting));

        Optional<String> first = service.getValue("expiry_warning_days");
        Optional<String> second = service.getValue("expiry_warning_days");

        assertEquals("30", first.orElseThrow());
        assertEquals("30", second.orElseThrow());
        verify(repository, times(1)).findBySettingKey("expiry_warning_days");
    }

    @Test
    void getValueShouldReturnEmptyWhenSettingMissing() {
        when(repository.findBySettingKey("missing")).thenReturn(Optional.empty());

        assertTrue(service.getValue("missing").isEmpty());
    }

    @Test
    void getExpiryWarningDaysShouldReturnConfiguredValue() {
        when(repository.findBySettingKey("expiry_warning_days"))
            .thenReturn(Optional.of(buildSetting("expiry_warning_days", "45")));

        assertEquals(45, service.getExpiryWarningDays());
    }

    @Test
    void getExpiryWarningDaysShouldFallBackToDefaultWhenSettingMissing() {
        when(repository.findBySettingKey("expiry_warning_days")).thenReturn(Optional.empty());

        assertEquals(SystemSettingService.DEFAULT_EXPIRY_WARNING_DAYS, service.getExpiryWarningDays());
    }

    @Test
    void getExpiryWarningDaysShouldFallBackToDefaultWhenValueIsNotInteger() {
        when(repository.findBySettingKey("expiry_warning_days"))
            .thenReturn(Optional.of(buildSetting("expiry_warning_days", "not-a-number")));

        assertEquals(SystemSettingService.DEFAULT_EXPIRY_WARNING_DAYS, service.getExpiryWarningDays());
    }

    @Test
    void updateShouldCreateNewSettingWhenMissing() {
        when(repository.findBySettingKey("expiry_warning_days")).thenReturn(Optional.empty());
        when(repository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        SystemSetting saved = service.update("expiry_warning_days", "60", 7L);

        verify(repository).save(settingCaptor.capture());
        SystemSetting captured = settingCaptor.getValue();
        assertEquals("expiry_warning_days", captured.getSettingKey());
        assertEquals("60", captured.getSettingValue());
        assertEquals(7L, captured.getUpdatedBy());
        assertEquals("60", saved.getSettingValue());
    }

    @Test
    void updateShouldOverwriteExistingSettingAndRefreshCache() {
        SystemSetting existing = buildSetting("expiry_warning_days", "30");
        when(repository.findBySettingKey("expiry_warning_days")).thenReturn(Optional.of(existing));
        when(repository.save(any(SystemSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update("expiry_warning_days", "90", 9L);

        verify(repository).save(settingCaptor.capture());
        assertEquals("90", settingCaptor.getValue().getSettingValue());
        assertEquals(9L, settingCaptor.getValue().getUpdatedBy());

        // After update, getValue should hit cache, not the repository.
        Optional<String> cached = service.getValue("expiry_warning_days");
        assertEquals("90", cached.orElseThrow());
        verify(repository, times(1)).findBySettingKey("expiry_warning_days");
    }

    @Test
    void updateShouldRejectNonIntegerExpiryWarningDays() {
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.update("expiry_warning_days", "abc", 1L)
        );
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldRejectExpiryWarningDaysOutOfRange() {
        assertThrows(
            BusinessException.class,
            () -> service.update("expiry_warning_days", "-1", 1L)
        );
        assertThrows(
            BusinessException.class,
            () -> service.update("expiry_warning_days", "9999", 1L)
        );
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldRejectBlankKey() {
        assertThrows(BusinessException.class, () -> service.update(" ", "30", 1L));
        assertThrows(BusinessException.class, () -> service.update(null, "30", 1L));
    }

    private SystemSetting buildSetting(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setId(1L);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setUpdatedAt(OffsetDateTime.now());
        return setting;
    }
}
