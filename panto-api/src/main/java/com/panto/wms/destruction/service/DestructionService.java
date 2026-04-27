package com.panto.wms.destruction.service;

import com.panto.wms.audit.annotation.Auditable;
import com.panto.wms.audit.domain.AuditAction;
import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.destruction.dto.CreateDestructionRequest;
import com.panto.wms.destruction.dto.DestructionPageResponse;
import com.panto.wms.destruction.dto.DestructionResponse;
import com.panto.wms.destruction.dto.DestructionSummaryResponse;
import com.panto.wms.destruction.entity.Destruction;
import com.panto.wms.destruction.repository.DestructionRepository;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.entity.InventoryTransaction;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.repository.InventoryTransactionRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 销毁业务服务。
 */
@Slf4j
@Service
public class DestructionService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DESTRUCTION_DOCUMENT_TYPE = "DESTRUCTION";

    private final DestructionRepository destructionRepository;
    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * 创建销毁业务服务。
     *
     * @param destructionRepository 销毁记录仓储
     * @param batchRepository 批次仓储
     * @param productRepository 产品仓储
     * @param inventoryTransactionRepository 库存事务仓储
     */
    public DestructionService(
        DestructionRepository destructionRepository,
        BatchRepository batchRepository,
        ProductRepository productRepository,
        InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.destructionRepository = destructionRepository;
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    /**
     * 创建销毁记录，并扣减指定批次库存。
     *
     * @param request 创建请求体
     * @param operatorId 当前操作人 ID
     * @return 创建后的销毁详情
     */
    @Transactional
    @Auditable(
        action = AuditAction.CREATE,
        entityType = "DESTRUCTION",
        entityClass = Destruction.class,
        entityId = "#result.id",
        description = "创建销毁记录"
    )
    public DestructionResponse createDestruction(CreateDestructionRequest request, Long operatorId) {
        try {
            return doCreateDestruction(request, operatorId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn(
                "销毁批次时发生库存并发冲突, batchId={}, operatorId={}",
                request.batchId(),
                operatorId,
                ex
            );
            throw new BusinessException(ErrorCode.DESTRUCTION_STOCK_CONFLICT);
        }
    }

    /**
     * 分页查询销毁记录列表。
     *
     * @param productId 产品 ID，可为空
     * @param dateFrom 起始日期，可为空
     * @param dateTo 结束日期，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页销毁记录
     */
    @Transactional(readOnly = true)
    public DestructionPageResponse listDestructions(
        Long productId,
        LocalDate dateFrom,
        LocalDate dateTo,
        int page,
        int size
    ) {
        PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<Destruction> result = destructionRepository.findAll(
            buildFilterSpecification(productId, toStartOfDay(dateFrom), toStartOfNextDay(dateTo)),
            pageRequest
        );

        Map<Long, Batch> batchMap = loadBatchMap(
            result.getContent().stream().map(Destruction::getBatchId).distinct().toList()
        );
        Map<Long, Product> productMap = loadProductMap(
            result.getContent().stream().map(Destruction::getProductId).distinct().toList()
        );

        List<DestructionSummaryResponse> items = result.getContent().stream()
            .map(destruction -> toSummaryResponse(
                destruction,
                batchMap.get(destruction.getBatchId()),
                productMap.get(destruction.getProductId())
            ))
            .toList();

        return new DestructionPageResponse(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    /**
     * 查询销毁记录详情。
     *
     * @param destructionId 销毁记录 ID
     * @return 销毁详情
     */
    @Transactional(readOnly = true)
    public DestructionResponse getDestruction(Long destructionId) {
        Destruction destruction = findDestructionOrThrow(destructionId);
        Batch batch = batchRepository.findById(destruction.getBatchId()).orElse(null);
        Product product = productRepository.findById(destruction.getProductId()).orElse(null);
        return toResponse(destruction, batch, product);
    }

    private DestructionResponse doCreateDestruction(CreateDestructionRequest request, Long operatorId) {
        Batch batch = batchRepository.findById(request.batchId())
            .orElseThrow(() -> new BusinessException(ErrorCode.DESTRUCTION_BATCH_NOT_FOUND));
        if (batch.getQuantityRemaining() < request.quantityDestroyed()) {
            throw new BusinessException(
                ErrorCode.DESTRUCTION_INSUFFICIENT_STOCK,
                "批次 [%s] 当前剩余库存为 %d，不足以销毁 %d".formatted(
                    batch.getBatchNumber(),
                    batch.getQuantityRemaining(),
                    request.quantityDestroyed()
                )
            );
        }

        Product product = productRepository.findById(batch.getProductId())
            .orElseThrow(() -> new IllegalStateException("Missing product for batch " + batch.getId()));

        OffsetDateTime now = OffsetDateTime.now();
        int quantityBefore = batch.getQuantityRemaining();
        int quantityAfter = quantityBefore - request.quantityDestroyed();

        batch.setQuantityRemaining(quantityAfter);
        batch.setUpdatedAt(now);
        batch.setUpdatedBy(operatorId);
        batchRepository.save(batch);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setBatchId(batch.getId());
        transaction.setProductId(batch.getProductId());
        transaction.setTransactionType(TransactionType.DESTROY);
        transaction.setQuantityDelta(-request.quantityDestroyed());
        transaction.setQuantityBefore(quantityBefore);
        transaction.setQuantityAfter(quantityAfter);
        transaction.setRelatedDocumentType(DESTRUCTION_DOCUMENT_TYPE);
        transaction.setNote("Destroy batch #" + batch.getBatchNumber());
        transaction.setCreatedAt(now);
        transaction.setCreatedBy(operatorId);
        InventoryTransaction savedTransaction = inventoryTransactionRepository.saveAndFlush(transaction);

        Destruction destruction = new Destruction();
        destruction.setDestructionNumber(generateDestructionNumber(now));
        destruction.setBatchId(batch.getId());
        destruction.setProductId(batch.getProductId());
        destruction.setInventoryTransactionId(savedTransaction.getId());
        destruction.setQuantityDestroyed(request.quantityDestroyed());
        destruction.setPurchaseUnitPriceSnapshot(batch.getPurchaseUnitPrice());
        destruction.setLossAmount(calculateLossAmount(batch.getPurchaseUnitPrice(), request.quantityDestroyed()));
        destruction.setReason(request.reason().trim());
        destruction.setCreatedAt(now);
        destruction.setCreatedBy(operatorId);
        Destruction savedDestruction = destructionRepository.save(destruction);

        savedTransaction.setRelatedDocumentId(savedDestruction.getId());
        inventoryTransactionRepository.save(savedTransaction);

        batchRepository.flush();

        return toResponse(savedDestruction, batch, product);
    }

    private Destruction findDestructionOrThrow(Long destructionId) {
        return destructionRepository.findById(destructionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DESTRUCTION_NOT_FOUND));
    }

    private Map<Long, Batch> loadBatchMap(Collection<Long> batchIds) {
        if (batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchRepository.findAllById(batchIds).stream()
            .collect(Collectors.toMap(Batch::getId, batch -> batch, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Product> loadProductMap(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
    }

    private String generateDestructionNumber(OffsetDateTime now) {
        LocalDate localDate = now.toLocalDate();
        OffsetDateTime start = localDate.atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime end = start.plusDays(1);
        long count = destructionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        return String.format("DES-%s-%03d", localDate.format(DATE_FORMAT), count + 1);
    }

    private DestructionSummaryResponse toSummaryResponse(Destruction destruction, Batch batch, Product product) {
        return new DestructionSummaryResponse(
            destruction.getId(),
            destruction.getDestructionNumber(),
            destruction.getBatchId(),
            batch != null ? batch.getBatchNumber() : null,
            batch != null ? batch.getExpiryDate() : null,
            batch != null ? batch.getExpiryStatus() : null,
            destruction.getProductId(),
            product != null ? product.getSku() : null,
            product != null ? product.getName() : null,
            destruction.getQuantityDestroyed(),
            destruction.getPurchaseUnitPriceSnapshot(),
            destruction.getLossAmount(),
            destruction.getReason(),
            destruction.getCreatedAt(),
            destruction.getCreatedBy()
        );
    }

    private DestructionResponse toResponse(Destruction destruction, Batch batch, Product product) {
        return new DestructionResponse(
            destruction.getId(),
            destruction.getDestructionNumber(),
            destruction.getBatchId(),
            batch != null ? batch.getBatchNumber() : null,
            batch != null ? batch.getExpiryDate() : null,
            batch != null ? batch.getExpiryStatus() : null,
            batch != null ? batch.getQuantityRemaining() : null,
            destruction.getProductId(),
            product != null ? product.getSku() : null,
            product != null ? product.getName() : null,
            destruction.getInventoryTransactionId(),
            destruction.getQuantityDestroyed(),
            destruction.getPurchaseUnitPriceSnapshot(),
            destruction.getLossAmount(),
            destruction.getReason(),
            destruction.getCreatedAt(),
            destruction.getCreatedBy()
        );
    }

    private Specification<Destruction> buildFilterSpecification(
        Long productId,
        OffsetDateTime createdAtFrom,
        OffsetDateTime createdAtTo
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), productId));
            }
            if (createdAtFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
            }
            if (createdAtTo != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), createdAtTo));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(currentOffset());
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().atOffset(currentOffset());
    }

    private ZoneOffset currentOffset() {
        return OffsetDateTime.now().getOffset();
    }

    private BigDecimal calculateLossAmount(BigDecimal purchaseUnitPrice, int quantityDestroyed) {
        return purchaseUnitPrice.multiply(BigDecimal.valueOf(quantityDestroyed)).setScale(2, RoundingMode.HALF_UP);
    }
}
