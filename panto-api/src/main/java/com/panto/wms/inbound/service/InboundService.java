package com.panto.wms.inbound.service;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.inbound.dto.CreateInboundRequest;
import com.panto.wms.inbound.dto.InboundDetailResponse;
import com.panto.wms.inbound.dto.InboundItemRequest;
import com.panto.wms.inbound.dto.InboundItemResponse;
import com.panto.wms.inbound.dto.InboundPageResponse;
import com.panto.wms.inbound.dto.InboundSummaryResponse;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 入库单业务服务。
 */
@Slf4j
@Service
public class InboundService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InboundRecordRepository inboundRecordRepository;
    private final InboundItemRepository inboundItemRepository;
    private final BatchRepository batchRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;

    /**
     * 创建入库单服务。
     */
    public InboundService(
        InboundRecordRepository inboundRecordRepository,
        InboundItemRepository inboundItemRepository,
        BatchRepository batchRepository,
        InventoryTransactionRepository inventoryTransactionRepository,
        ProductRepository productRepository
    ) {
        this.inboundRecordRepository = inboundRecordRepository;
        this.inboundItemRepository = inboundItemRepository;
        this.batchRepository = batchRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productRepository = productRepository;
    }

    /**
     * 分页查询入库单列表。
     *
     * @param dateFrom  起始日期，可为空
     * @param dateTo    结束日期，可为空
     * @param productId 产品 ID 筛选，可为空
     * @param page      页码
     * @param size      每页条数
     * @return 分页入库单列表
     */
    @Transactional(readOnly = true)
    public InboundPageResponse listInbounds(
        LocalDate dateFrom, LocalDate dateTo, Long productId, int page, int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<InboundRecord> records = inboundRecordRepository.search(dateFrom, dateTo, productId, pageRequest);

        List<Long> recordIds = records.getContent().stream().map(InboundRecord::getId).toList();
        Map<Long, Long> itemCountMap = loadItemCountMap(recordIds);

        List<InboundSummaryResponse> items = records.getContent().stream()
            .map(r -> toSummaryResponse(r, itemCountMap.getOrDefault(r.getId(), 0L).intValue()))
            .toList();

        return new InboundPageResponse(
            items,
            records.getNumber(),
            records.getSize(),
            records.getTotalElements(),
            records.getTotalPages()
        );
    }

    /**
     * 查询入库单详情。
     *
     * @param id 入库单 ID
     * @return 入库单详情（含明细行）
     */
    @Transactional(readOnly = true)
    public InboundDetailResponse getInbound(Long id) {
        InboundRecord record = findRecordOrThrow(id);
        List<InboundItem> items = inboundItemRepository.findByInboundRecordId(id);
        Map<Long, Product> productMap = loadProductMap(
            items.stream().map(InboundItem::getProductId).distinct().toList()
        );
        return toDetailResponse(record, items, productMap);
    }

    /**
     * 创建入库单，同时生成批次和库存事务（IN）。
     *
     * @param request    创建请求
     * @param operatorId 当前操作人 ID
     * @return 创建后的入库单详情
     */
    @Transactional
    public InboundDetailResponse createInbound(CreateInboundRequest request, Long operatorId) {
        List<Long> productIds = extractProductIds(request.items());
        Map<Long, Product> productMap = loadProductMap(productIds);
        validateAllProductsExist(productIds, productMap);

        OffsetDateTime now = OffsetDateTime.now();

        InboundRecord record = new InboundRecord();
        record.setInboundNumber(generateInboundNumber(request.inboundDate()));
        record.setInboundDate(request.inboundDate());
        record.setRemarks(request.remarks());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.setCreatedBy(operatorId);
        record.setUpdatedBy(operatorId);
        InboundRecord savedRecord = inboundRecordRepository.save(record);

        List<InboundItem> savedItems = createItemsBatchesAndTransactions(
            savedRecord.getId(), request.items(), productMap, request.inboundDate(), now, operatorId
        );

        return toDetailResponse(savedRecord, savedItems, productMap);
    }

    /**
     * 更新入库单。仅在该入库单所有批次库存未被消耗时允许修改。
     *
     * @param id         入库单 ID
     * @param request    更新请求
     * @param operatorId 当前操作人 ID
     * @return 更新后的入库单详情
     */
    @Transactional
    public InboundDetailResponse updateInbound(Long id, UpdateInboundRequest request, Long operatorId) {
        InboundRecord record = findRecordOrThrow(id);

        List<InboundItem> existingItems = inboundItemRepository.findByInboundRecordId(id);
        List<Long> existingItemIds = existingItems.stream().map(InboundItem::getId).toList();
        List<Batch> existingBatches = batchRepository.findByInboundItemIdIn(existingItemIds);

        boolean anyConsumed = existingBatches.stream()
            .anyMatch(b -> !b.getQuantityRemaining().equals(b.getQuantityReceived()));
        if (anyConsumed) {
            throw new BusinessException(ErrorCode.INBOUND_HAS_STOCK_MOVEMENT);
        }

        List<Long> batchIds = existingBatches.stream().map(Batch::getId).toList();
        if (!batchIds.isEmpty()) {
            inventoryTransactionRepository.deleteByBatchIdIn(batchIds);
            batchRepository.deleteAll(existingBatches);
        }
        inboundItemRepository.deleteByInboundRecordId(id);

        List<Long> productIds = extractProductIds(request.items());
        Map<Long, Product> productMap = loadProductMap(productIds);
        validateAllProductsExist(productIds, productMap);

        OffsetDateTime now = OffsetDateTime.now();
        record.setInboundDate(request.inboundDate());
        record.setRemarks(request.remarks());
        record.setUpdatedAt(now);
        record.setUpdatedBy(operatorId);
        InboundRecord savedRecord = inboundRecordRepository.save(record);

        List<InboundItem> savedItems = createItemsBatchesAndTransactions(
            savedRecord.getId(), request.items(), productMap, request.inboundDate(), now, operatorId
        );

        return toDetailResponse(savedRecord, savedItems, productMap);
    }

    private List<InboundItem> createItemsBatchesAndTransactions(
        Long recordId,
        List<InboundItemRequest> itemRequests,
        Map<Long, Product> productMap,
        LocalDate arrivalDate,
        OffsetDateTime now,
        Long operatorId
    ) {
        List<InboundItem> savedItems = new ArrayList<>();

        for (InboundItemRequest req : itemRequests) {
            Product product = productMap.get(req.productId());
            String batchNumber = generateBatchNumber(product.getSku(), arrivalDate, product.getId());

            InboundItem item = new InboundItem();
            item.setInboundRecordId(recordId);
            item.setProductId(req.productId());
            item.setBatchNumber(batchNumber);
            item.setExpiryDate(req.expiryDate());
            item.setQuantity(req.quantity());
            item.setPurchaseUnitPrice(req.purchaseUnitPrice());
            item.setRemarks(req.remarks());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            item.setCreatedBy(operatorId);
            item.setUpdatedBy(operatorId);
            InboundItem savedItem = inboundItemRepository.save(item);
            savedItems.add(savedItem);

            Batch batch = new Batch();
            batch.setProductId(req.productId());
            batch.setInboundItemId(savedItem.getId());
            batch.setBatchNumber(batchNumber);
            batch.setArrivalDate(arrivalDate);
            batch.setExpiryDate(req.expiryDate());
            batch.setQuantityReceived(req.quantity());
            batch.setQuantityRemaining(req.quantity());
            batch.setPurchaseUnitPrice(req.purchaseUnitPrice());
            batch.setExpiryStatus(ExpiryStatus.NORMAL);
            batch.setCreatedAt(now);
            batch.setUpdatedAt(now);
            batch.setCreatedBy(operatorId);
            batch.setUpdatedBy(operatorId);
            Batch savedBatch = batchRepository.save(batch);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setBatchId(savedBatch.getId());
            tx.setProductId(req.productId());
            tx.setTransactionType(TransactionType.IN);
            tx.setQuantityDelta(req.quantity());
            tx.setQuantityBefore(0);
            tx.setQuantityAfter(req.quantity());
            tx.setRelatedDocumentType("INBOUND");
            tx.setRelatedDocumentId(recordId);
            tx.setNote("Goods received, inbound record #" + recordId);
            tx.setCreatedAt(now);
            tx.setCreatedBy(operatorId);
            inventoryTransactionRepository.save(tx);
        }

        return savedItems;
    }

    private String generateInboundNumber(LocalDate date) {
        long count = inboundRecordRepository.countByInboundDate(date);
        return String.format("IN-%s-%03d", date.format(DATE_FORMAT), count + 1);
    }

    private String generateBatchNumber(String sku, LocalDate arrivalDate, Long productId) {
        long count = batchRepository.countByProductIdAndArrivalDate(productId, arrivalDate);
        return String.format("%s-%s-%03d", sku, arrivalDate.format(DATE_FORMAT), count + 1);
    }

    private InboundRecord findRecordOrThrow(Long id) {
        return inboundRecordRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.INBOUND_NOT_FOUND));
    }

    private List<Long> extractProductIds(List<InboundItemRequest> items) {
        return items.stream().map(InboundItemRequest::productId).distinct().toList();
    }

    private Map<Long, Product> loadProductMap(Collection<Long> productIds) {
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
    }

    private void validateAllProductsExist(List<Long> productIds, Map<Long, Product> productMap) {
        productIds.forEach(pid -> {
            if (!productMap.containsKey(pid)) {
                throw new BusinessException(ErrorCode.INBOUND_PRODUCT_NOT_FOUND);
            }
        });
    }

    private Map<Long, Long> loadItemCountMap(List<Long> recordIds) {
        if (recordIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return inboundItemRepository.findByInboundRecordIdIn(recordIds).stream()
            .collect(Collectors.groupingBy(InboundItem::getInboundRecordId, Collectors.counting()));
    }

    private InboundSummaryResponse toSummaryResponse(InboundRecord record, int itemCount) {
        return new InboundSummaryResponse(
            record.getId(),
            record.getInboundNumber(),
            record.getInboundDate(),
            itemCount,
            record.getRemarks(),
            record.getCreatedAt(),
            record.getCreatedBy()
        );
    }

    private InboundDetailResponse toDetailResponse(
        InboundRecord record, List<InboundItem> items, Map<Long, Product> productMap
    ) {
        List<InboundItemResponse> itemResponses = items.stream()
            .map(item -> {
                Product product = productMap.get(item.getProductId());
                return new InboundItemResponse(
                    item.getId(),
                    item.getProductId(),
                    product != null ? product.getSku() : null,
                    product != null ? product.getName() : null,
                    item.getBatchNumber(),
                    item.getExpiryDate(),
                    item.getQuantity(),
                    item.getPurchaseUnitPrice(),
                    item.getRemarks()
                );
            })
            .toList();

        return new InboundDetailResponse(
            record.getId(),
            record.getInboundNumber(),
            record.getInboundDate(),
            record.getRemarks(),
            itemResponses,
            record.getCreatedAt(),
            record.getUpdatedAt(),
            record.getCreatedBy(),
            record.getUpdatedBy()
        );
    }
}
