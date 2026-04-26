package com.panto.wms.dashboard.service;

import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.dashboard.dto.DashboardSummaryResponse;
import com.panto.wms.destruction.repository.DestructionRepository;
import com.panto.wms.inbound.repository.InboundRecordRepository;
import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.service.InventoryQueryService;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 看板汇总业务服务。
 */
@Service
public class DashboardService {

    private final InventoryQueryService inventoryQueryService;
    private final InboundRecordRepository inboundRecordRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DestructionRepository destructionRepository;
    private final BatchRepository batchRepository;

    /**
     * 创建看板汇总业务服务。
     *
     * @param inventoryQueryService 库存查询服务
     * @param inboundRecordRepository 入库记录仓储
     * @param orderRepository 订单仓储
     * @param orderItemRepository 订单明细仓储
     * @param destructionRepository 销毁记录仓储
     * @param batchRepository 批次仓储
     */
    public DashboardService(
        InventoryQueryService inventoryQueryService,
        InboundRecordRepository inboundRecordRepository,
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        DestructionRepository destructionRepository,
        BatchRepository batchRepository
    ) {
        this.inventoryQueryService = inventoryQueryService;
        this.inboundRecordRepository = inboundRecordRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.destructionRepository = destructionRepository;
        this.batchRepository = batchRepository;
    }

    /**
     * 按角色生成看板汇总数据。
     *
     * @param role 当前用户角色
     * @return 看板汇总
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UserRole role) {
        DashboardSummaryResponse.WarningSummary warnings = buildWarnings();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime todayStart = toStartOfDay(now.toLocalDate(), now.getOffset());
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);
        OffsetDateTime monthStart = toStartOfDay(now.toLocalDate().withDayOfMonth(1), now.getOffset());
        OffsetDateTime nextMonthStart = monthStart.plusMonths(1);

        return switch (role) {
            case ADMIN, MARKETING -> new DashboardSummaryResponse(
                role,
                warnings,
                buildManagerSummary(warnings, todayStart, tomorrowStart, monthStart, nextMonthStart),
                null,
                null
            );
            case WAREHOUSE -> new DashboardSummaryResponse(
                role,
                warnings,
                null,
                buildWarehouseSummary(warnings, todayStart, tomorrowStart),
                null
            );
            case ACCOUNTANT -> new DashboardSummaryResponse(
                role,
                warnings,
                null,
                null,
                buildAccountantSummary(monthStart, nextMonthStart)
            );
        };
    }

    private DashboardSummaryResponse.WarningSummary buildWarnings() {
        int lowStockCount = inventoryQueryService.getLowStockProducts().size();
        List<Batch> activeBatches = batchRepository.findAllActive();

        int expiringSoonCount = (int) activeBatches.stream()
            .filter(batch -> batch.getExpiryStatus() == ExpiryStatus.EXPIRING_SOON)
            .count();
        int expiredCount = (int) activeBatches.stream()
            .filter(batch -> batch.getExpiryStatus() == ExpiryStatus.EXPIRED)
            .count();

        return new DashboardSummaryResponse.WarningSummary(lowStockCount, expiringSoonCount, expiredCount);
    }

    private DashboardSummaryResponse.ManagerSummary buildManagerSummary(
        DashboardSummaryResponse.WarningSummary warnings,
        OffsetDateTime todayStart,
        OffsetDateTime tomorrowStart,
        OffsetDateTime monthStart,
        OffsetDateTime nextMonthStart
    ) {
        BigDecimal todaySalesTotal = sumSales(todayStart, tomorrowStart);
        BigDecimal monthSalesTotal = sumSales(monthStart, nextMonthStart);
        int pendingTaskCount = warnings.lowStockCount() + warnings.expiringSoonCount() + warnings.expiredCount();

        return new DashboardSummaryResponse.ManagerSummary(
            todaySalesTotal,
            monthSalesTotal,
            pendingTaskCount,
            buildTopProducts(monthStart, nextMonthStart)
        );
    }

    private DashboardSummaryResponse.WarehouseSummary buildWarehouseSummary(
        DashboardSummaryResponse.WarningSummary warnings,
        OffsetDateTime todayStart,
        OffsetDateTime tomorrowStart
    ) {
        long todayInboundCount = inboundRecordRepository.countByInboundDate(todayStart.toLocalDate());
        long todayOutboundCount = orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus.ACTIVE,
            todayStart,
            tomorrowStart
        ).size();

        return new DashboardSummaryResponse.WarehouseSummary(
            todayInboundCount,
            todayOutboundCount,
            warnings.expiredCount()
        );
    }

    private DashboardSummaryResponse.AccountantSummary buildAccountantSummary(
        OffsetDateTime monthStart,
        OffsetDateTime nextMonthStart
    ) {
        return new DashboardSummaryResponse.AccountantSummary(
            sumSales(monthStart, nextMonthStart),
            normalizeAmount(destructionRepository.sumLossInRange(monthStart, nextMonthStart))
        );
    }

    private List<DashboardSummaryResponse.TopProductSummary> buildTopProducts(
        OffsetDateTime monthStart,
        OffsetDateTime nextMonthStart
    ) {
        List<Order> monthlyOrders = orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus.ACTIVE,
            monthStart,
            nextMonthStart
        );
        if (monthlyOrders.isEmpty()) {
            return List.of();
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(
            monthlyOrders.stream().map(Order::getId).toList()
        );
        Map<Long, TopProductAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            TopProductAccumulator accumulator = accumulatorMap.computeIfAbsent(
                item.getProductId(),
                ignored -> new TopProductAccumulator(
                    item.getProductId(),
                    item.getProductSkuSnapshot(),
                    item.getProductNameSnapshot()
                )
            );
            accumulator.quantitySold += item.getQuantity();
            accumulator.salesAmount = accumulator.salesAmount.add(item.getSubtotal().add(item.getGstAmount()));
        }

        return accumulatorMap.values().stream()
            .sorted(Comparator
                .comparingInt(TopProductAccumulator::quantitySold).reversed()
                .thenComparing(TopProductAccumulator::salesAmount, Comparator.reverseOrder())
                .thenComparing(TopProductAccumulator::productName))
            .limit(10)
            .map(accumulator -> new DashboardSummaryResponse.TopProductSummary(
                accumulator.productId(),
                accumulator.productSku(),
                accumulator.productName(),
                accumulator.quantitySold(),
                normalizeAmount(accumulator.salesAmount())
            ))
            .toList();
    }

    private BigDecimal sumSales(OffsetDateTime start, OffsetDateTime end) {
        return normalizeAmount(orderRepository.sumTotalAmountByStatusAndCreatedAtRange(OrderStatus.ACTIVE, start, end));
    }

    private OffsetDateTime toStartOfDay(LocalDate date, ZoneOffset offset) {
        return date.atStartOfDay().atOffset(offset);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return Optional.ofNullable(value)
            .orElse(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private static final class TopProductAccumulator {
        private final Long productId;
        private final String productSku;
        private final String productName;
        private int quantitySold;
        private BigDecimal salesAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        private TopProductAccumulator(Long productId, String productSku, String productName) {
            this.productId = productId;
            this.productSku = productSku;
            this.productName = productName;
        }

        private Long productId() {
            return productId;
        }

        private String productSku() {
            return productSku;
        }

        private String productName() {
            return productName;
        }

        private int quantitySold() {
            return quantitySold;
        }

        private BigDecimal salesAmount() {
            return salesAmount;
        }
    }
}
