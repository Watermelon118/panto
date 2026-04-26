package com.panto.wms.dashboard.dto;

import com.panto.wms.auth.domain.UserRole;
import java.math.BigDecimal;
import java.util.List;

/**
 * 看板汇总响应体。
 *
 * @param role 当前用户角色
 * @param warnings 预警摘要，所有角色可见
 * @param managerSummary 经理/销售看板摘要，仅 ADMIN/MARKETING 返回
 * @param warehouseSummary 仓库看板摘要，仅 WAREHOUSE 返回
 * @param accountantSummary 财务看板摘要，仅 ACCOUNTANT 返回
 */
public record DashboardSummaryResponse(
    UserRole role,
    WarningSummary warnings,
    ManagerSummary managerSummary,
    WarehouseSummary warehouseSummary,
    AccountantSummary accountantSummary
) {

    /**
     * 库存预警摘要。
     *
     * @param lowStockCount 低库存商品数
     * @param expiringSoonCount 临期批次数
     * @param expiredCount 已过期且仍有库存的批次数
     */
    public record WarningSummary(
        int lowStockCount,
        int expiringSoonCount,
        int expiredCount
    ) {
    }

    /**
     * 经理/销售看板摘要。
     *
     * @param todaySalesTotal 今日销售总额
     * @param monthSalesTotal 本月销售总额
     * @param pendingTaskCount 待处理事项数
     * @param topProducts 本月销量前 10 商品
     */
    public record ManagerSummary(
        BigDecimal todaySalesTotal,
        BigDecimal monthSalesTotal,
        int pendingTaskCount,
        List<TopProductSummary> topProducts
    ) {
    }

    /**
     * 仓库看板摘要。
     *
     * @param todayInboundCount 今日入库单数
     * @param todayOutboundCount 今日出库单数
     * @param pendingDestructionCount 待销毁批次数
     */
    public record WarehouseSummary(
        long todayInboundCount,
        long todayOutboundCount,
        int pendingDestructionCount
    ) {
    }

    /**
     * 财务看板摘要。
     *
     * @param monthSalesTotal 本月销售总额
     * @param monthLossTotal 本月损耗总额
     */
    public record AccountantSummary(
        BigDecimal monthSalesTotal,
        BigDecimal monthLossTotal
    ) {
    }

    /**
     * 热销商品摘要。
     *
     * @param productId 商品 ID
     * @param productSku 商品 SKU
     * @param productName 商品名称
     * @param quantitySold 本月销量
     * @param salesAmount 本月销售额
     */
    public record TopProductSummary(
        Long productId,
        String productSku,
        String productName,
        int quantitySold,
        BigDecimal salesAmount
    ) {
    }
}
