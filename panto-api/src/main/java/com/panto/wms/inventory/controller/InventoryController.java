package com.panto.wms.inventory.controller;

import com.panto.wms.common.api.Result;
import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.dto.BatchPageResponse;
import com.panto.wms.inventory.dto.BatchResponse;
import com.panto.wms.inventory.dto.StockPageResponse;
import com.panto.wms.inventory.dto.StockSummaryResponse;
import com.panto.wms.inventory.dto.TransactionPageResponse;
import com.panto.wms.inventory.service.InventoryQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存查询相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryQueryService inventoryQueryService;

    /**
     * 创建库存控制器。
     *
     * @param inventoryQueryService 库存查询业务服务
     */
    public InventoryController(InventoryQueryService inventoryQueryService) {
        this.inventoryQueryService = inventoryQueryService;
    }

    /**
     * 返回分页的产品库存汇总，支持关键字和分类筛选。
     *
     * @param keyword  SKU 或名称关键字，可为空
     * @param category 分类筛选，可为空
     * @param page     页码
     * @param size     每页条数
     * @return 分页产品库存汇总
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<StockPageResponse> getStockSummary(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(inventoryQueryService.getStockSummary(keyword, category, page, size));
    }

    /**
     * 返回分页的批次列表，支持产品和到期状态筛选。
     *
     * @param productId    产品 ID 筛选，可为空
     * @param expiryStatus 到期状态筛选（NORMAL / EXPIRING_SOON / EXPIRED），可为空
     * @param page         页码
     * @param size         每页条数
     * @return 分页批次列表
     */
    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<BatchPageResponse> getBatches(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) ExpiryStatus expiryStatus,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(inventoryQueryService.getBatches(productId, expiryStatus, page, size));
    }

    /**
     * 返回分页的库存事务记录，支持产品和事务类型筛选。
     *
     * @param productId       产品 ID 筛选，可为空
     * @param transactionType 事务类型筛选（IN / OUT / ROLLBACK / DESTROY / ADJUST），可为空
     * @param page            页码
     * @param size            每页条数
     * @return 分页库存事务列表
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<TransactionPageResponse> getTransactions(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) TransactionType transactionType,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(inventoryQueryService.getTransactions(productId, transactionType, page, size));
    }

    /**
     * 返回当前库存低于安全库存的产品列表，按当前库存升序排列。
     *
     * @return 低库存产品列表
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<List<StockSummaryResponse>> getLowStockProducts() {
        return Result.success(inventoryQueryService.getLowStockProducts());
    }

    /**
     * 返回指定天数内即将到期且仍有剩余库存的批次列表。
     *
     * @param withinDays 未来天数阈值，默认 30 天，最大 365 天
     * @return 临期批次列表
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')")
    public Result<List<BatchResponse>> getExpiringBatches(
        @RequestParam(defaultValue = "30") @Min(1) @Max(365) int withinDays
    ) {
        return Result.success(inventoryQueryService.getExpiringBatches(withinDays));
    }
}
