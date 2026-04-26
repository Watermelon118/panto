package com.panto.wms.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.dashboard.dto.DashboardSummaryResponse;
import com.panto.wms.destruction.repository.DestructionRepository;
import com.panto.wms.inbound.repository.InboundRecordRepository;
import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.dto.StockSummaryResponse;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.service.InventoryQueryService;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 看板汇总业务服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private InventoryQueryService inventoryQueryService;
    @Mock private InboundRecordRepository inboundRecordRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private DestructionRepository destructionRepository;
    @Mock private BatchRepository batchRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryShouldReturnManagerFieldsForAdmin() {
        when(inventoryQueryService.getLowStockProducts()).thenReturn(List.of(
            new StockSummaryResponse(1L, "SKU-1", "Low A", "Frozen", "carton", 10, 3, true),
            new StockSummaryResponse(2L, "SKU-2", "Low B", "Frozen", "carton", 8, 2, true)
        ));
        when(batchRepository.findAllActive()).thenReturn(List.of(
            buildBatch(ExpiryStatus.EXPIRING_SOON),
            buildBatch(ExpiryStatus.EXPIRED),
            buildBatch(ExpiryStatus.NORMAL)
        ));
        when(orderRepository.sumTotalAmountByStatusAndCreatedAtRange(eq(OrderStatus.ACTIVE), any(), any()))
            .thenReturn(new BigDecimal("1280.50"), new BigDecimal("5666.80"));
        when(orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(OrderStatus.ACTIVE), any(), any()))
            .thenReturn(List.of(buildOrder(10L)));
        when(orderItemRepository.findByOrderIdIn(List.of(10L))).thenReturn(List.of(
            buildOrderItem(100L, 10L, 5L, "SKU-5", "Frozen Dumplings", 7, new BigDecimal("140.00"), new BigDecimal("21.00")),
            buildOrderItem(101L, 10L, 6L, "SKU-6", "Spring Rolls", 3, new BigDecimal("60.00"), new BigDecimal("9.00"))
        ));

        DashboardSummaryResponse response = dashboardService.getSummary(UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, response.role());
        assertNotNull(response.warnings());
        assertEquals(2, response.warnings().lowStockCount());
        assertEquals(1, response.warnings().expiringSoonCount());
        assertEquals(1, response.warnings().expiredCount());

        assertNotNull(response.managerSummary());
        assertNull(response.warehouseSummary());
        assertNull(response.accountantSummary());
        assertEquals(new BigDecimal("1280.50"), response.managerSummary().todaySalesTotal());
        assertEquals(new BigDecimal("5666.80"), response.managerSummary().monthSalesTotal());
        assertEquals(4, response.managerSummary().pendingTaskCount());
        assertEquals(2, response.managerSummary().topProducts().size());
        assertEquals("Frozen Dumplings", response.managerSummary().topProducts().getFirst().productName());
        assertEquals(7, response.managerSummary().topProducts().getFirst().quantitySold());
        assertEquals(new BigDecimal("161.00"), response.managerSummary().topProducts().getFirst().salesAmount());
    }

    @Test
    void getSummaryShouldReturnWarehouseFieldsForWarehouseRole() {
        when(inventoryQueryService.getLowStockProducts()).thenReturn(List.of(
            new StockSummaryResponse(1L, "SKU-1", "Low A", "Frozen", "carton", 10, 3, true)
        ));
        when(batchRepository.findAllActive()).thenReturn(List.of(
            buildBatch(ExpiryStatus.EXPIRED),
            buildBatch(ExpiryStatus.EXPIRING_SOON)
        ));
        when(inboundRecordRepository.countByInboundDate(any(LocalDate.class))).thenReturn(3L);
        when(orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(OrderStatus.ACTIVE), any(), any()))
            .thenReturn(List.of(buildOrder(10L), buildOrder(11L)));

        DashboardSummaryResponse response = dashboardService.getSummary(UserRole.WAREHOUSE);

        assertEquals(UserRole.WAREHOUSE, response.role());
        assertNotNull(response.warnings());
        assertNull(response.managerSummary());
        assertNull(response.accountantSummary());
        assertNotNull(response.warehouseSummary());
        assertEquals(3L, response.warehouseSummary().todayInboundCount());
        assertEquals(2L, response.warehouseSummary().todayOutboundCount());
        assertEquals(1, response.warehouseSummary().pendingDestructionCount());
    }

    @Test
    void getSummaryShouldReturnAccountantFieldsForAccountantRole() {
        when(inventoryQueryService.getLowStockProducts()).thenReturn(List.of());
        when(batchRepository.findAllActive()).thenReturn(List.of(buildBatch(ExpiryStatus.NORMAL)));
        when(orderRepository.sumTotalAmountByStatusAndCreatedAtRange(eq(OrderStatus.ACTIVE), any(), any()))
            .thenReturn(new BigDecimal("8800.25"));
        when(destructionRepository.sumLossInRange(any(), any())).thenReturn(new BigDecimal("320.40"));

        DashboardSummaryResponse response = dashboardService.getSummary(UserRole.ACCOUNTANT);

        assertEquals(UserRole.ACCOUNTANT, response.role());
        assertNotNull(response.warnings());
        assertNull(response.managerSummary());
        assertNull(response.warehouseSummary());
        assertNotNull(response.accountantSummary());
        assertEquals(new BigDecimal("8800.25"), response.accountantSummary().monthSalesTotal());
        assertEquals(new BigDecimal("320.40"), response.accountantSummary().monthLossTotal());
    }

    private Batch buildBatch(ExpiryStatus expiryStatus) {
        Batch batch = new Batch();
        batch.setId(1L);
        batch.setProductId(5L);
        batch.setInboundItemId(9L);
        batch.setBatchNumber("BATCH-001");
        batch.setArrivalDate(LocalDate.now().minusDays(10));
        batch.setExpiryDate(LocalDate.now().plusDays(5));
        batch.setQuantityReceived(10);
        batch.setQuantityRemaining(5);
        batch.setPurchaseUnitPrice(new BigDecimal("12.50"));
        batch.setExpiryStatus(expiryStatus);
        batch.setVersion(0);
        batch.setCreatedAt(OffsetDateTime.now().minusDays(10));
        batch.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        batch.setCreatedBy(1L);
        batch.setUpdatedBy(1L);
        return batch;
    }

    private Order buildOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber("ORD-20260426-" + id);
        order.setCustomerId(1L);
        order.setStatus(OrderStatus.ACTIVE);
        order.setSubtotalAmount(new BigDecimal("140.00"));
        order.setGstAmount(new BigDecimal("21.00"));
        order.setTotalAmount(new BigDecimal("161.00"));
        order.setCreatedAt(OffsetDateTime.now().minusHours(2));
        order.setUpdatedAt(OffsetDateTime.now().minusHours(2));
        order.setCreatedBy(7L);
        order.setUpdatedBy(7L);
        return order;
    }

    private OrderItem buildOrderItem(
        Long id,
        Long orderId,
        Long productId,
        String sku,
        String name,
        int quantity,
        BigDecimal subtotal,
        BigDecimal gstAmount
    ) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setBatchId(100L + id);
        item.setProductSkuSnapshot(sku);
        item.setProductNameSnapshot(name);
        item.setProductUnitSnapshot("carton");
        item.setProductSpecSnapshot("1kg x 10");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setSubtotal(subtotal);
        item.setGstApplicable(true);
        item.setGstAmount(gstAmount);
        item.setCreatedAt(OffsetDateTime.now().minusHours(1));
        return item;
    }
}
