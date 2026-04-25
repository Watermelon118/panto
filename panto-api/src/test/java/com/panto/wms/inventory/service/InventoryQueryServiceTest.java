package com.panto.wms.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.dto.BatchPageResponse;
import com.panto.wms.inventory.dto.BatchResponse;
import com.panto.wms.inventory.dto.StockPageResponse;
import com.panto.wms.inventory.dto.StockSummaryResponse;
import com.panto.wms.inventory.dto.TransactionPageResponse;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.entity.InventoryTransaction;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.repository.InventoryTransactionRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import com.panto.wms.product.repository.ProductStockView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 库存查询服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private InventoryQueryService inventoryQueryService;

    // ── getStockSummary ──────────────────────────────────────────────────────

    @Test
    void getStockSummaryShouldReturnPageWithBelowSafetyStockFlag() {
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 50);
        PageImpl<Product> page = new PageImpl<>(
            List.of(product),
            PageRequest.of(0, 20, Sort.by("name").ascending()),
            1
        );

        when(productRepository.search(null, null, true,
            PageRequest.of(0, 20, Sort.by("name").ascending()))).thenReturn(page);
        when(productRepository.findCurrentStockByProductIds(List.of(1L)))
            .thenReturn(List.of(stockView(1L, 30L)));

        StockPageResponse response = inventoryQueryService.getStockSummary(null, null, 0, 20);

        assertEquals(1, response.items().size());
        StockSummaryResponse item = response.items().getFirst();
        assertEquals(1L, item.productId());
        assertEquals("APPLE001", item.sku());
        assertEquals(30L, item.currentStock());
        assertEquals(50, item.safetyStock());
        assertTrue(item.belowSafetyStock());
        assertEquals(1L, response.totalElements());
    }

    @Test
    void getStockSummaryShouldMarkBelowSafetyStockFalseWhenStockIsSufficient() {
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 20);
        PageImpl<Product> page = new PageImpl<>(
            List.of(product),
            PageRequest.of(0, 20, Sort.by("name").ascending()),
            1
        );

        when(productRepository.search(null, null, true,
            PageRequest.of(0, 20, Sort.by("name").ascending()))).thenReturn(page);
        when(productRepository.findCurrentStockByProductIds(List.of(1L)))
            .thenReturn(List.of(stockView(1L, 100L)));

        StockPageResponse response = inventoryQueryService.getStockSummary(null, null, 0, 20);

        assertFalse(response.items().getFirst().belowSafetyStock());
        assertEquals(100L, response.items().getFirst().currentStock());
    }

    @Test
    void getStockSummaryShouldDefaultToZeroStockWhenNoбатчesExist() {
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 10);
        PageImpl<Product> page = new PageImpl<>(
            List.of(product),
            PageRequest.of(0, 20, Sort.by("name").ascending()),
            1
        );

        when(productRepository.search(null, null, true,
            PageRequest.of(0, 20, Sort.by("name").ascending()))).thenReturn(page);
        when(productRepository.findCurrentStockByProductIds(List.of(1L)))
            .thenReturn(List.of());

        StockPageResponse response = inventoryQueryService.getStockSummary(null, null, 0, 20);

        assertEquals(0L, response.items().getFirst().currentStock());
        assertTrue(response.items().getFirst().belowSafetyStock());
    }

    // ── getBatches ───────────────────────────────────────────────────────────

    @Test
    void getBatchesShouldReturnPageWithProductInfo() {
        Batch batch = buildBatch(10L, 1L, "APPLE001-20250425-001", 100, 80);
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 50);
        PageImpl<Batch> page = new PageImpl<>(List.of(batch), PageRequest.of(0, 20), 1);

        when(batchRepository.search(null, null, PageRequest.of(0, 20))).thenReturn(page);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        BatchPageResponse response = inventoryQueryService.getBatches(null, null, 0, 20);

        assertEquals(1, response.items().size());
        BatchResponse item = response.items().getFirst();
        assertEquals(10L, item.id());
        assertEquals("APPLE001-20250425-001", item.batchNumber());
        assertEquals("APPLE001", item.productSku());
        assertEquals("Green Apple", item.productName());
        assertEquals(100, item.quantityReceived());
        assertEquals(80, item.quantityRemaining());
        assertEquals(ExpiryStatus.NORMAL, item.expiryStatus());
    }

    @Test
    void getBatchesShouldFilterByExpiryStatus() {
        Batch batch = buildBatch(10L, 1L, "APPLE001-20250425-001", 100, 100);
        batch.setExpiryStatus(ExpiryStatus.EXPIRING_SOON);
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 50);
        PageImpl<Batch> page = new PageImpl<>(List.of(batch), PageRequest.of(0, 20), 1);

        when(batchRepository.search(eq(1L), eq(ExpiryStatus.EXPIRING_SOON), any())).thenReturn(page);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        BatchPageResponse response = inventoryQueryService.getBatches(1L, ExpiryStatus.EXPIRING_SOON, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals(ExpiryStatus.EXPIRING_SOON, response.items().getFirst().expiryStatus());
    }

    // ── getTransactions ──────────────────────────────────────────────────────

    @Test
    void getTransactionsShouldReturnPageWithBatchNumberAndProductInfo() {
        InventoryTransaction tx = buildTransaction(100L, 10L, 1L, TransactionType.IN, 80, 0, 80);
        Batch batch = buildBatch(10L, 1L, "APPLE001-20250425-001", 80, 80);
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 50);
        PageImpl<InventoryTransaction> page = new PageImpl<>(List.of(tx), PageRequest.of(0, 20), 1);

        when(inventoryTransactionRepository.search(null, null, PageRequest.of(0, 20))).thenReturn(page);
        when(batchRepository.findAllById(List.of(10L))).thenReturn(List.of(batch));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        TransactionPageResponse response = inventoryQueryService.getTransactions(null, null, 0, 20);

        assertEquals(1, response.items().size());
        var item = response.items().getFirst();
        assertEquals(100L, item.id());
        assertEquals("APPLE001-20250425-001", item.batchNumber());
        assertEquals("APPLE001", item.productSku());
        assertEquals(TransactionType.IN, item.transactionType());
        assertEquals(80, item.quantityDelta());
        assertEquals(0, item.quantityBefore());
        assertEquals(80, item.quantityAfter());
    }

    // ── getLowStockProducts ──────────────────────────────────────────────────

    @Test
    void getLowStockProductsShouldReturnOnlyProductsBelowSafetyStock() {
        Product low = buildProduct(1L, "APPLE001", "Green Apple", 50);
        Product ok = buildProduct(2L, "ORANGE001", "Orange", 20);

        when(productRepository.findByActiveTrue()).thenReturn(List.of(low, ok));
        when(productRepository.findCurrentStockByProductIds(List.of(1L, 2L)))
            .thenReturn(List.of(stockView(1L, 10L), stockView(2L, 100L)));

        List<StockSummaryResponse> result = inventoryQueryService.getLowStockProducts();

        assertEquals(1, result.size());
        assertEquals("APPLE001", result.getFirst().sku());
        assertEquals(10L, result.getFirst().currentStock());
        assertTrue(result.getFirst().belowSafetyStock());
    }

    @Test
    void getLowStockProductsShouldReturnEmptyWhenAllStockIsSufficient() {
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 20);

        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));
        when(productRepository.findCurrentStockByProductIds(List.of(1L)))
            .thenReturn(List.of(stockView(1L, 100L)));

        List<StockSummaryResponse> result = inventoryQueryService.getLowStockProducts();

        assertTrue(result.isEmpty());
    }

    @Test
    void getLowStockProductsShouldSortByCurrentStockAscending() {
        Product p1 = buildProduct(1L, "APPLE001", "Green Apple", 100);
        Product p2 = buildProduct(2L, "BANANA001", "Banana", 100);

        when(productRepository.findByActiveTrue()).thenReturn(List.of(p1, p2));
        when(productRepository.findCurrentStockByProductIds(List.of(1L, 2L)))
            .thenReturn(List.of(stockView(1L, 30L), stockView(2L, 5L)));

        List<StockSummaryResponse> result = inventoryQueryService.getLowStockProducts();

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).currentStock());
        assertEquals(30L, result.get(1).currentStock());
    }

    // ── getExpiringBatches ───────────────────────────────────────────────────

    @Test
    void getExpiringBatchesShouldReturnBatchesWithProductInfo() {
        Batch batch = buildBatch(10L, 1L, "APPLE001-20250425-001", 100, 60);
        Product product = buildProduct(1L, "APPLE001", "Green Apple", 50);

        when(batchRepository.findExpiringBatches(any(LocalDate.class))).thenReturn(List.of(batch));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        List<BatchResponse> result = inventoryQueryService.getExpiringBatches(30);

        assertEquals(1, result.size());
        assertEquals("APPLE001-20250425-001", result.getFirst().batchNumber());
        assertEquals("APPLE001", result.getFirst().productSku());
        assertEquals(60, result.getFirst().quantityRemaining());
    }

    @Test
    void getExpiringBatchesShouldReturnEmptyWhenNoBatchesExpiringSoon() {
        when(batchRepository.findExpiringBatches(any(LocalDate.class))).thenReturn(List.of());

        List<BatchResponse> result = inventoryQueryService.getExpiringBatches(30);

        assertTrue(result.isEmpty());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Product buildProduct(Long id, String sku, String name, int safetyStock) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setCategory("Fruit");
        product.setUnit("carton");
        product.setReferencePurchasePrice(new BigDecimal("2.50"));
        product.setReferenceSalePrice(new BigDecimal("4.00"));
        product.setSafetyStock(safetyStock);
        product.setGstApplicable(false);
        product.setActive(true);
        product.setCreatedAt(OffsetDateTime.now().minusDays(30));
        product.setUpdatedAt(OffsetDateTime.now().minusDays(30));
        product.setCreatedBy(1L);
        product.setUpdatedBy(1L);
        return product;
    }

    private Batch buildBatch(Long id, Long productId, String batchNumber, int received, int remaining) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProductId(productId);
        batch.setInboundItemId(100L);
        batch.setBatchNumber(batchNumber);
        batch.setArrivalDate(LocalDate.of(2025, 4, 25));
        batch.setExpiryDate(LocalDate.of(2025, 10, 25));
        batch.setQuantityReceived(received);
        batch.setQuantityRemaining(remaining);
        batch.setPurchaseUnitPrice(new BigDecimal("2.50"));
        batch.setExpiryStatus(ExpiryStatus.NORMAL);
        batch.setCreatedAt(OffsetDateTime.now().minusDays(1));
        batch.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        batch.setCreatedBy(1L);
        batch.setUpdatedBy(1L);
        return batch;
    }

    private InventoryTransaction buildTransaction(
        Long id, Long batchId, Long productId,
        TransactionType type, int delta, int before, int after
    ) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setId(id);
        tx.setBatchId(batchId);
        tx.setProductId(productId);
        tx.setTransactionType(type);
        tx.setQuantityDelta(delta);
        tx.setQuantityBefore(before);
        tx.setQuantityAfter(after);
        tx.setRelatedDocumentType("INBOUND");
        tx.setRelatedDocumentId(1L);
        tx.setNote("Test transaction");
        tx.setCreatedAt(OffsetDateTime.now().minusHours(1));
        tx.setCreatedBy(1L);
        return tx;
    }

    private ProductStockView stockView(Long productId, Long currentStock) {
        return new ProductStockView() {
            @Override public Long getProductId() { return productId; }
            @Override public Long getCurrentStock() { return currentStock; }
        };
    }
}
