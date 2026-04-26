package com.panto.wms.reports.controller;

import com.panto.wms.reports.service.ReportService;
import java.time.LocalDate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务报表导出 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    /**
     * 创建报表导出控制器。
     *
     * @param reportService 报表导出业务服务
     */
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 导出销售报表。
     *
     * @param from 起始日期（含）
     * @param to 结束日期（含）
     * @param format 导出格式，支持 csv / xlsx
     * @return 文件下载响应
     */
    @GetMapping("/sales/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ByteArrayResource> exportSales(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "xlsx") String format
    ) {
        ReportService.ReportFile reportFile = reportService.exportSales(from, to, format);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(reportFile.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportFile.fileName() + "\"")
            .contentLength(reportFile.content().length)
            .body(new ByteArrayResource(reportFile.content()));
    }

    /**
     * 导出损耗报表。
     *
     * @param from 起始日期（含）
     * @param to 结束日期（含）
     * @param format 导出格式，支持 csv / xlsx
     * @return 文件下载响应
     */
    @GetMapping("/losses/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ByteArrayResource> exportLosses(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "xlsx") String format
    ) {
        ReportService.ReportFile reportFile = reportService.exportLosses(from, to, format);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(reportFile.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportFile.fileName() + "\"")
            .contentLength(reportFile.content().length)
            .body(new ByteArrayResource(reportFile.content()));
    }
}
