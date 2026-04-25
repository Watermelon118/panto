package com.panto.wms.inbound.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.inbound.dto.CreateInboundRequest;
import com.panto.wms.inbound.dto.InboundDetailResponse;
import com.panto.wms.inbound.dto.InboundItemRequest;
import com.panto.wms.inbound.dto.InboundPageResponse;
import com.panto.wms.inbound.dto.UpdateInboundRequest;
import com.panto.wms.inbound.entity.InboundItem;
import com.panto.wms.inbound.entity.InboundRecord;
import com.panto.wms.inbound.repository.InboundItemRepository;
import com.panto.wms.inbound.repository.InboundRecordRepository;
import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.entity.InventoryTransaction;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.repository.InventoryTransactionRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 入库单服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InboundServiceTest {

    private static final LocalDate INBOUND_DATE = LocalDate.of(2025, 4, 25);
    private static final Long OPERATOR_ID = 1L;

    @Mock private InboundRecordRepository inboundRecordRepository;
    @Mock private InboundItemRepository inboundItemRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private InboundService inboundService;

    @Captor private ArgumentCaptor<InboundRecord> recordCaptor;
    @Captor private ArgumentCaptor<InboundItem> itemCaptor;
    @Captor private ArgumentCaptor<Batch> batchCaptor;
    @Captor private ArgumentCaptor<InventoryTransaction> txCaptor;

    // ── createInbound ────────────────────────────────────────────────────────

    @Test
    void createInboundShouldSaveRecordItemBatchAndTransactionWhenRequestIsValid() {
        CreateInboundRequest request = buildCreateRequest();
        Product product = buildProduct(10L, "APPLE001", "Green Apple");

        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(product));
        when(inboundRecordRepository.countByInboundDate(INBOUND_DATE)).thenReturn(0L);
        when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> {
            InboundRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(inboundItemRepository.save(any(InboundItem.class))).thenAnswer(inv -> {
            InboundItem i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });
        when(batchRepository.countByProductIdAndArrivalDate(10L, INBOUND_DATE)).thenReturn(0L);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId(20L);
            return b;
        });
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        InboundDetailResponse response = inboundService.createInbound(request, OPERATOR_ID);

        assertEquals(1L, response.id());
        assertEquals("IN-20250425-001", response.inboundNumber());
        assertEquals(INBOUND_DATE, response.inboundDate());
        assertEquals(1, response.items().size());
        assertEquals("APPLE001-20250425-001", response.items().getFirst().batchNumber());
        assertEquals("APPLE001", response.items().getFirst().productSku());

        verify(inboundRecordRepository).save(recordCaptor.capture());
        InboundRecord savedRecord = recordCaptor.getValue();
        assertEquals("IN-20250425-001", savedRecord.getInboundNumber());
        assertEquals(INBOUND_DATE, savedRecord.getInboundDate());
        assertEquals(OPERATOR_ID, savedRecord.getCreatedBy());
        assertNotNull(savedRecord.getCreatedAt());

        verify(inboundItemRepository).save(itemCaptor.capture());
        InboundItem savedItem = itemCaptor.getValue();
        assertEquals(10L, savedItem.getProductId());
        assertEquals(100, savedItem.getQuantity());
        assertEquals(new BigDecimal("2.50"), savedItem.getPurchaseUnitPrice());

        verify(batchRepository).save(batchCaptor.capture());
        Batch savedBatch = batchCaptor.getValue();
        assertEquals("APPLE001-20250425-001", savedBatch.getBatchNumber());
        assertEquals(100, savedBatch.getQuantityReceived());
        assertEquals(100, savedBatch.getQuantityRemaining());
        assertEquals(ExpiryStatus.NORMAL, savedBatch.getExpiryStatus());
        assertEquals(10L, savedBatch.getInboundItemId());

        verify(inventoryTransactionRepository).save(txCaptor.capture());
        InventoryTransaction savedTx = txCaptor.getValue();
        assertEquals(TransactionType.IN, savedTx.getTransactionType());
        assertEquals(100, savedTx.getQuantityDelta());
        assertEquals(0, savedTx.getQuantityBefore());
        assertEquals(100, savedTx.getQuantityAfter());
        assertEquals("INBOUND", savedTx.getRelatedDocumentType());
        assertEquals(1L, savedTx.getRelatedDocumentId());
    }

    @Test
    void createInboundShouldThrowWhenProductDoesNotExist() {
        CreateInboundRequest request = buildCreateRequest();
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> inboundService.createInbound(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.INBOUND_PRODUCT_NOT_FOUND, ex.getErrorCode());
        verify(inboundRecordRepository, never()).save(any());
    }

    @Test
    void createInboundShouldGenerateSequentialNumbersOnSameDate() {
        CreateInboundRequest request = buildCreateRequest();
        Product product = buildProduct(10L, "APPLE001", "Green Apple");

        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(product));
        when(inboundRecordRepository.countByInboundDate(INBOUND_DATE)).thenReturn(4L);
        when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> {
            InboundRecord r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });
        when(inboundItemRepository.save(any(InboundItem.class))).thenAnswer(inv -> {
            InboundItem i = inv.getArgument(0);
            i.setId(50L);
            return i;
        });
        when(batchRepository.countByProductIdAndArrivalDate(10L, INBOUND_DATE)).thenReturn(2L);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId(60L);
            return b;
        });
        when(inventoryTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InboundDetailResponse response = inboundService.createInbound(request, OPERATOR_ID);

        assertEquals("IN-20250425-005", response.inboundNumber());
        assertEquals("APPLE001-20250425-003", response.items().getFirst().batchNumber());
    }

    // ── getInbound ───────────────────────────────────────────────────────────

    @Test
    void getInboundShouldReturnDetailWithItemsAndProductInfo() {
        InboundRecord record = buildRecord(1L, "IN-20250425-001");
        InboundItem item = buildItem(10L, 1L, 10L, "APPLE001-20250425-001");
        Product product = buildProduct(10L, "APPLE001", "Green Apple");

        when(inboundRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(inboundItemRepository.findByInboundRecordId(1L)).thenReturn(List.of(item));
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(product));

        InboundDetailResponse response = inboundService.getInbound(1L);

        assertEquals(1L, response.id());
        assertEquals("IN-20250425-001", response.inboundNumber());
        assertEquals(1, response.items().size());
        assertEquals("APPLE001", response.items().getFirst().productSku());
        assertEquals("Green Apple", response.items().getFirst().productName());
        assertEquals("APPLE001-20250425-001", response.items().getFirst().batchNumber());
    }

    @Test
    void getInboundShouldThrowWhenRecordNotFound() {
        when(inboundRecordRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> inboundService.getInbound(99L)
        );

        assertEquals(ErrorCode.INBOUND_NOT_FOUND, ex.getErrorCode());
    }

    // ── updateInbound ────────────────────────────────────────────────────────

    @Test
    void updateInboundShouldThrowWhenAnyBatchHasBeenConsumed() {
        InboundRecord record = buildRecord(1L, "IN-20250425-001");
        InboundItem item = buildItem(10L, 1L, 10L, "APPLE001-20250425-001");
        Batch consumedBatch = buildBatch(20L, 10L, 100, 80);

        when(inboundRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(inboundItemRepository.findByInboundRecordId(1L)).thenReturn(List.of(item));
        when(batchRepository.findByInboundItemIdIn(List.of(10L))).thenReturn(List.of(consumedBatch));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> inboundService.updateInbound(1L, buildUpdateRequest(), OPERATOR_ID)
        );

        assertEquals(ErrorCode.INBOUND_HAS_STOCK_MOVEMENT, ex.getErrorCode());
        verify(inventoryTransactionRepository, never()).deleteByBatchIdIn(any());
        verify(inboundItemRepository, never()).deleteByInboundRecordId(any());
    }

    @Test
    void updateInboundShouldDeleteAndRecreateBatchesWhenNoStockConsumed() {
        InboundRecord record = buildRecord(1L, "IN-20250425-001");
        InboundItem existingItem = buildItem(10L, 1L, 10L, "APPLE001-20250425-001");
        Batch intactBatch = buildBatch(20L, 10L, 100, 100);
        Product product = buildProduct(10L, "APPLE001", "Green Apple");

        when(inboundRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(inboundItemRepository.findByInboundRecordId(1L)).thenReturn(List.of(existingItem));
        when(batchRepository.findByInboundItemIdIn(List.of(10L))).thenReturn(List.of(intactBatch));
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(product));
        when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inboundItemRepository.save(any(InboundItem.class))).thenAnswer(inv -> {
            InboundItem i = inv.getArgument(0);
            i.setId(11L);
            return i;
        });
        when(batchRepository.countByProductIdAndArrivalDate(10L, INBOUND_DATE)).thenReturn(0L);
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> {
            Batch b = inv.getArgument(0);
            b.setId(21L);
            return b;
        });
        when(inventoryTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inboundService.updateInbound(1L, buildUpdateRequest(), OPERATOR_ID);

        verify(inventoryTransactionRepository).deleteByBatchIdIn(List.of(20L));
        verify(batchRepository).deleteAll(List.of(intactBatch));
        verify(inboundItemRepository).deleteByInboundRecordId(1L);
        verify(inboundItemRepository).save(any(InboundItem.class));
        verify(batchRepository).save(any(Batch.class));
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void updateInboundShouldThrowWhenRecordNotFound() {
        when(inboundRecordRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> inboundService.updateInbound(99L, buildUpdateRequest(), OPERATOR_ID)
        );

        assertEquals(ErrorCode.INBOUND_NOT_FOUND, ex.getErrorCode());
    }

    // ── listInbounds ─────────────────────────────────────────────────────────

    @Test
    void listInboundsShouldReturnPagedResultsWithCorrectItemCounts() {
        InboundRecord record = buildRecord(1L, "IN-20250425-001");
        PageImpl<InboundRecord> page = new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1);
        InboundItem item1 = buildItem(10L, 1L, 10L, "APPLE001-20250425-001");
        InboundItem item2 = buildItem(11L, 1L, 10L, "APPLE001-20250425-002");

        when(inboundRecordRepository.search(null, null, null, PageRequest.of(0, 20))).thenReturn(page);
        when(inboundItemRepository.findByInboundRecordIdIn(List.of(1L))).thenReturn(List.of(item1, item2));

        InboundPageResponse response = inboundService.listInbounds(null, null, null, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals("IN-20250425-001", response.items().getFirst().inboundNumber());
        assertEquals(2, response.items().getFirst().itemCount());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CreateInboundRequest buildCreateRequest() {
        return new CreateInboundRequest(
            INBOUND_DATE,
            "Test inbound",
            List.of(new InboundItemRequest(
                10L,
                LocalDate.of(2025, 10, 25),
                100,
                new BigDecimal("2.50"),
                null
            ))
        );
    }

    private UpdateInboundRequest buildUpdateRequest() {
        return new UpdateInboundRequest(
            INBOUND_DATE,
            "Updated remarks",
            List.of(new InboundItemRequest(
                10L,
                LocalDate.of(2025, 11, 1),
                150,
                new BigDecimal("2.80"),
                null
            ))
        );
    }

    private InboundRecord buildRecord(Long id, String inboundNumber) {
        InboundRecord record = new InboundRecord();
        record.setId(id);
        record.setInboundNumber(inboundNumber);
        record.setInboundDate(INBOUND_DATE);
        record.setRemarks("Test");
        record.setCreatedAt(OffsetDateTime.now().minusDays(1));
        record.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        record.setCreatedBy(OPERATOR_ID);
        record.setUpdatedBy(OPERATOR_ID);
        return record;
    }

    private InboundItem buildItem(Long id, Long recordId, Long productId, String batchNumber) {
        InboundItem item = new InboundItem();
        item.setId(id);
        item.setInboundRecordId(recordId);
        item.setProductId(productId);
        item.setBatchNumber(batchNumber);
        item.setExpiryDate(LocalDate.of(2025, 10, 25));
        item.setQuantity(100);
        item.setPurchaseUnitPrice(new BigDecimal("2.50"));
        item.setCreatedAt(OffsetDateTime.now().minusDays(1));
        item.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        item.setCreatedBy(OPERATOR_ID);
        item.setUpdatedBy(OPERATOR_ID);
        return item;
    }

    private Batch buildBatch(Long id, Long itemId, int received, int remaining) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setInboundItemId(itemId);
        batch.setProductId(10L);
        batch.setBatchNumber("APPLE001-20250425-001");
        batch.setArrivalDate(INBOUND_DATE);
        batch.setExpiryDate(LocalDate.of(2025, 10, 25));
        batch.setQuantityReceived(received);
        batch.setQuantityRemaining(remaining);
        batch.setPurchaseUnitPrice(new BigDecimal("2.50"));
        batch.setExpiryStatus(ExpiryStatus.NORMAL);
        batch.setCreatedAt(OffsetDateTime.now().minusDays(1));
        batch.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        batch.setCreatedBy(OPERATOR_ID);
        batch.setUpdatedBy(OPERATOR_ID);
        return batch;
    }

    private Product buildProduct(Long id, String sku, String name) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setCategory("Fruit");
        product.setUnit("carton");
        product.setReferencePurchasePrice(new BigDecimal("2.50"));
        product.setReferenceSalePrice(new BigDecimal("4.00"));
        product.setSafetyStock(50);
        product.setGstApplicable(false);
        product.setActive(true);
        product.setCreatedAt(OffsetDateTime.now().minusDays(30));
        product.setUpdatedAt(OffsetDateTime.now().minusDays(30));
        product.setCreatedBy(OPERATOR_ID);
        product.setUpdatedBy(OPERATOR_ID);
        return product;
    }
}
