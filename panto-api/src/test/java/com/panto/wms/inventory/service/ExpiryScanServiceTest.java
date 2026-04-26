package com.panto.wms.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.settings.service.SystemSettingService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 到期扫描服务测试。
 */
@ExtendWith(MockitoExtension.class)
class ExpiryScanServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 26);

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private SystemSettingService settingService;

    @InjectMocks
    private ExpiryScanService scanService;

    @Test
    void runScanShouldClassifyExpiredBatchAsExpired() {
        Batch batch = batchWithExpiry(LocalDate.of(2026, 4, 25), ExpiryStatus.NORMAL);
        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of(batch));

        ExpiryScanService.ScanResult result = scanService.runScan(TODAY);

        assertEquals(ExpiryStatus.EXPIRED, batch.getExpiryStatus());
        assertEquals(1, result.scanned());
        assertEquals(1, result.updated());
        verify(batchRepository).save(batch);
    }

    @Test
    void runScanShouldClassifyBatchExpiringTodayAsExpiringSoon() {
        Batch batch = batchWithExpiry(TODAY, ExpiryStatus.NORMAL);
        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of(batch));

        scanService.runScan(TODAY);

        assertEquals(ExpiryStatus.EXPIRING_SOON, batch.getExpiryStatus());
        verify(batchRepository).save(batch);
    }

    @Test
    void runScanShouldClassifyBatchAtThresholdBoundaryAsExpiringSoon() {
        Batch batch = batchWithExpiry(TODAY.plusDays(30), ExpiryStatus.NORMAL);
        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of(batch));

        scanService.runScan(TODAY);

        assertEquals(ExpiryStatus.EXPIRING_SOON, batch.getExpiryStatus());
        verify(batchRepository).save(batch);
    }

    @Test
    void runScanShouldClassifyBatchOutsideWarningWindowAsNormal() {
        Batch batch = batchWithExpiry(TODAY.plusDays(31), ExpiryStatus.EXPIRING_SOON);
        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of(batch));

        scanService.runScan(TODAY);

        assertEquals(ExpiryStatus.NORMAL, batch.getExpiryStatus());
        verify(batchRepository).save(batch);
    }

    @Test
    void runScanShouldNotSaveBatchWhenStatusAlreadyMatchesExpected() {
        Batch alreadyNormal = batchWithExpiry(TODAY.plusDays(60), ExpiryStatus.NORMAL);
        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of(alreadyNormal));

        ExpiryScanService.ScanResult result = scanService.runScan(TODAY);

        assertEquals(1, result.scanned());
        assertEquals(0, result.updated());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void runScanShouldHandleMixedBatchesAndCountOnlyChangedAsUpdated() {
        Batch toExpired = batchWithExpiry(TODAY.minusDays(1), ExpiryStatus.NORMAL);
        Batch toWarning = batchWithExpiry(TODAY.plusDays(15), ExpiryStatus.NORMAL);
        Batch alreadyNormal = batchWithExpiry(TODAY.plusDays(90), ExpiryStatus.NORMAL);

        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of(toExpired, toWarning, alreadyNormal));

        ExpiryScanService.ScanResult result = scanService.runScan(TODAY);

        assertEquals(3, result.scanned());
        assertEquals(2, result.updated());
        assertEquals(ExpiryStatus.EXPIRED, toExpired.getExpiryStatus());
        assertEquals(ExpiryStatus.EXPIRING_SOON, toWarning.getExpiryStatus());
        assertEquals(ExpiryStatus.NORMAL, alreadyNormal.getExpiryStatus());
        verify(batchRepository, times(2)).save(any());
    }

    @Test
    void runScanShouldNoOpWhenNoActiveBatches() {
        when(settingService.getExpiryWarningDays()).thenReturn(30);
        when(batchRepository.findAllActive()).thenReturn(List.of());

        ExpiryScanService.ScanResult result = scanService.runScan(TODAY);

        assertEquals(0, result.scanned());
        assertEquals(0, result.updated());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void runScanShouldUseConfiguredThresholdToDistinguishWarningFromNormal() {
        Batch batch = batchWithExpiry(TODAY.plusDays(45), ExpiryStatus.NORMAL);
        when(settingService.getExpiryWarningDays()).thenReturn(60);
        when(batchRepository.findAllActive()).thenReturn(List.of(batch));

        scanService.runScan(TODAY);

        assertEquals(ExpiryStatus.EXPIRING_SOON, batch.getExpiryStatus());
        verify(batchRepository).save(batch);
    }

    private Batch batchWithExpiry(LocalDate expiryDate, ExpiryStatus initialStatus) {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setExpiryDate(expiryDate);
        batch.setExpiryStatus(initialStatus);
        return batch;
    }
}
