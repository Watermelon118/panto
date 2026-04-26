package com.panto.wms.destruction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.destruction.dto.CreateDestructionRequest;
import com.panto.wms.destruction.dto.DestructionPageResponse;
import com.panto.wms.destruction.dto.DestructionResponse;
import com.panto.wms.destruction.entity.Destruction;
import com.panto.wms.destruction.repository.DestructionRepository;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 销毁业务服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DestructionServiceTest {

    private static final Long OPERATOR_ID = 7L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long BATCH_ID = 100L;

    @Mock private DestructionRepository destructionRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private DestructionService destructionService;

    @Captor private ArgumentCaptor<Destruction> destructionCaptor;
    @Captor private ArgumentCaptor<InventoryTransaction> transactionSaveCaptor;
    @Captor private ArgumentCaptor<InventoryTransaction> transactionSaveAndFlushCaptor;

    @Test
    void createDestructionShouldDeductBatchAndPersistDestroyTransaction() {
        CreateDestructionRequest request = new CreateDestructionRequest(BATCH_ID, 4, "Expired stock");
        Batch batch = buildBatch(12);
        Product product = buildProduct();

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.saveAndFlush(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction transaction = invocation.getArgument(0);
            transaction.setId(900L);
            return transaction;
        });
        when(destructionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(0L);
        when(destructionRepository.save(any(Destruction.class))).thenAnswer(invocation -> {
            Destruction destruction = invocation.getArgument(0);
            destruction.setId(500L);
            return destruction;
        });
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DestructionResponse response = destructionService.createDestruction(request, OPERATOR_ID);

        assertTrue(response.destructionNumber().startsWith("DES-"));
        assertEquals(BATCH_ID, response.batchId());
        assertEquals("BATCH-001", response.batchNumber());
        assertEquals(PRODUCT_ID, response.productId());
        assertEquals("DUMP001", response.productSku());
        assertEquals(8, response.batchQuantityRemaining());
        assertEquals(4, response.quantityDestroyed());
        assertEquals(new BigDecimal("50.00"), response.lossAmount());
        assertEquals("Expired stock", response.reason());
        assertEquals(900L, response.inventoryTransactionId());
        assertNotNull(response.createdAt());

        assertEquals(8, batch.getQuantityRemaining());

        verify(destructionRepository).save(destructionCaptor.capture());
        Destruction savedDestruction = destructionCaptor.getValue();
        assertEquals(900L, savedDestruction.getInventoryTransactionId());
        assertEquals(new BigDecimal("12.50"), savedDestruction.getPurchaseUnitPriceSnapshot());
        assertEquals(new BigDecimal("50.00"), savedDestruction.getLossAmount());
        assertEquals("Expired stock", savedDestruction.getReason());

        verify(inventoryTransactionRepository).saveAndFlush(transactionSaveAndFlushCaptor.capture());
        InventoryTransaction createTransaction = transactionSaveAndFlushCaptor.getValue();
        assertEquals(TransactionType.DESTROY, createTransaction.getTransactionType());
        assertEquals(-4, createTransaction.getQuantityDelta());
        assertEquals(12, createTransaction.getQuantityBefore());
        assertEquals(8, createTransaction.getQuantityAfter());
        assertEquals("DESTRUCTION", createTransaction.getRelatedDocumentType());

        verify(inventoryTransactionRepository).save(transactionSaveCaptor.capture());
        InventoryTransaction updatedTransaction = transactionSaveCaptor.getValue();
        assertEquals(500L, updatedTransaction.getRelatedDocumentId());

        verify(batchRepository).flush();
    }

    @Test
    void createDestructionShouldThrowWhenBatchDoesNotExist() {
        CreateDestructionRequest request = new CreateDestructionRequest(BATCH_ID, 2, "Damaged packaging");
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> destructionService.createDestruction(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.DESTRUCTION_BATCH_NOT_FOUND, ex.getErrorCode());
        verify(inventoryTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDestructionShouldThrowWhenQuantityExceedsRemainingStock() {
        CreateDestructionRequest request = new CreateDestructionRequest(BATCH_ID, 20, "Expired stock");
        Batch batch = buildBatch(12);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> destructionService.createDestruction(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.DESTRUCTION_INSUFFICIENT_STOCK, ex.getErrorCode());
        verify(destructionRepository, never()).save(any());
    }

    @Test
    void createDestructionShouldTranslateOptimisticLockException() {
        CreateDestructionRequest request = new CreateDestructionRequest(BATCH_ID, 3, "Broken seal");
        Batch batch = buildBatch(12);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(buildProduct()));
        when(batchRepository.save(any(Batch.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException(Batch.class, BATCH_ID));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> destructionService.createDestruction(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.DESTRUCTION_STOCK_CONFLICT, ex.getErrorCode());
    }

    @Test
    void listDestructionsShouldReturnPagedSummaries() {
        Destruction destruction = buildDestruction(500L);
        PageImpl<Destruction> page = new PageImpl<>(List.of(destruction), PageRequest.of(0, 20), 1);

        when(destructionRepository.findByFilters(null, null, null, PageRequest.of(0, 20))).thenReturn(page);
        when(batchRepository.findAllById(List.of(BATCH_ID))).thenReturn(List.of(buildBatch(8)));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(buildProduct()));

        DestructionPageResponse response = destructionService.listDestructions(null, null, null, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals("DES-20260426-001", response.items().getFirst().destructionNumber());
        assertEquals("BATCH-001", response.items().getFirst().batchNumber());
        assertEquals("Frozen Dumplings", response.items().getFirst().productName());
        assertEquals(new BigDecimal("50.00"), response.items().getFirst().lossAmount());
    }

    @Test
    void getDestructionShouldReturnDetail() {
        Destruction destruction = buildDestruction(501L);
        Batch batch = buildBatch(8);
        Product product = buildProduct();

        when(destructionRepository.findById(501L)).thenReturn(Optional.of(destruction));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        DestructionResponse response = destructionService.getDestruction(501L);

        assertEquals("DES-20260426-001", response.destructionNumber());
        assertEquals(8, response.batchQuantityRemaining());
        assertEquals(900L, response.inventoryTransactionId());
        assertEquals("Expired stock", response.reason());
    }

    @Test
    void getDestructionShouldThrowWhenMissing() {
        when(destructionRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> destructionService.getDestruction(999L)
        );

        assertEquals(ErrorCode.DESTRUCTION_NOT_FOUND, ex.getErrorCode());
    }

    private Batch buildBatch(int quantityRemaining) {
        Batch batch = new Batch();
        batch.setId(BATCH_ID);
        batch.setProductId(PRODUCT_ID);
        batch.setInboundItemId(200L);
        batch.setBatchNumber("BATCH-001");
        batch.setArrivalDate(LocalDate.now().minusDays(14));
        batch.setExpiryDate(LocalDate.now().plusDays(3));
        batch.setQuantityReceived(20);
        batch.setQuantityRemaining(quantityRemaining);
        batch.setPurchaseUnitPrice(new BigDecimal("12.50"));
        batch.setExpiryStatus(ExpiryStatus.EXPIRING_SOON);
        batch.setVersion(0);
        batch.setCreatedAt(OffsetDateTime.now().minusDays(14));
        batch.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        batch.setCreatedBy(OPERATOR_ID);
        batch.setUpdatedBy(OPERATOR_ID);
        return batch;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSku("DUMP001");
        product.setName("Frozen Dumplings");
        product.setCategory("Frozen");
        product.setSpecification("1kg x 10");
        product.setUnit("carton");
        product.setReferencePurchasePrice(new BigDecimal("12.50"));
        product.setReferenceSalePrice(new BigDecimal("18.80"));
        product.setSafetyStock(5);
        product.setGstApplicable(true);
        product.setActive(true);
        product.setCreatedAt(OffsetDateTime.now().minusDays(30));
        product.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        product.setCreatedBy(OPERATOR_ID);
        product.setUpdatedBy(OPERATOR_ID);
        return product;
    }

    private Destruction buildDestruction(Long id) {
        Destruction destruction = new Destruction();
        destruction.setId(id);
        destruction.setDestructionNumber("DES-20260426-001");
        destruction.setBatchId(BATCH_ID);
        destruction.setProductId(PRODUCT_ID);
        destruction.setInventoryTransactionId(900L);
        destruction.setQuantityDestroyed(4);
        destruction.setPurchaseUnitPriceSnapshot(new BigDecimal("12.50"));
        destruction.setLossAmount(new BigDecimal("50.00"));
        destruction.setReason("Expired stock");
        destruction.setCreatedAt(OffsetDateTime.now().minusHours(2));
        destruction.setCreatedBy(OPERATOR_ID);
        return destruction;
    }
}
