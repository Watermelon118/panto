package com.panto.wms.inventory.service;

import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.dto.BatchPageResponse;
import com.panto.wms.inventory.dto.BatchResponse;
import com.panto.wms.inventory.dto.InventoryTransactionResponse;
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
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存查询业务服务。
 */
@Slf4j
@Service
public class InventoryQueryService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * 创建库存查询服务。
     */
    public InventoryQueryService(
        ProductRepository productRepository,
        BatchRepository batchRepository,
        InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
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
    @Transactional(readOnly = true)
    public StockPageResponse getStockSummary(String keyword, String category, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> products = productRepository.search(
            toLikePattern(keyword),
            toLowerCaseValue(category),
            true,
            pageRequest
        );

        List<Long> productIds = products.getContent().stream().map(Product::getId).toList();
        Map<Long, Long> stockMap = loadStockMap(productIds);

        List<StockSummaryResponse> items = products.getContent().stream()
            .map(p -> toStockSummary(p, stockMap.getOrDefault(p.getId(), 0L)))
            .toList();

        return new StockPageResponse(
            items,
            products.getNumber(),
            products.getSize(),
            products.getTotalElements(),
            products.getTotalPages()
        );
    }

    /**
     * 返回分页的批次列表，支持产品和到期状态筛选。
     *
     * @param productId    产品 ID 筛选，可为空
     * @param expiryStatus 到期状态筛选，可为空
     * @param page         页码
     * @param size         每页条数
     * @return 分页批次列表
     */
    @Transactional(readOnly = true)
    public BatchPageResponse getBatches(Long productId, ExpiryStatus expiryStatus, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Batch> batches = batchRepository.search(productId, expiryStatus, pageRequest);

        Map<Long, Product> productMap = loadProductMap(
            batches.getContent().stream().map(Batch::getProductId).distinct().toList()
        );

        List<BatchResponse> items = batches.getContent().stream()
            .map(b -> toBatchResponse(b, productMap.get(b.getProductId())))
            .toList();

        return new BatchPageResponse(
            items,
            batches.getNumber(),
            batches.getSize(),
            batches.getTotalElements(),
            batches.getTotalPages()
        );
    }

    /**
     * 返回分页的库存事务记录，支持产品和事务类型筛选。
     *
     * @param productId       产品 ID 筛选，可为空
     * @param transactionType 事务类型筛选，可为空
     * @param page            页码
     * @param size            每页条数
     * @return 分页库存事务列表
     */
    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(
        Long productId, TransactionType transactionType, int page, int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<InventoryTransaction> txPage =
            inventoryTransactionRepository.search(productId, transactionType, pageRequest);

        List<Long> batchIds = txPage.getContent().stream().map(InventoryTransaction::getBatchId).distinct().toList();
        List<Long> productIds = txPage.getContent().stream().map(InventoryTransaction::getProductId).distinct().toList();

        Map<Long, Batch> batchMap = loadBatchMap(batchIds);
        Map<Long, Product> productMap = loadProductMap(productIds);

        List<InventoryTransactionResponse> items = txPage.getContent().stream()
            .map(tx -> toTransactionResponse(tx, batchMap.get(tx.getBatchId()), productMap.get(tx.getProductId())))
            .toList();

        return new TransactionPageResponse(
            items,
            txPage.getNumber(),
            txPage.getSize(),
            txPage.getTotalElements(),
            txPage.getTotalPages()
        );
    }

    /**
     * 返回当前库存低于安全库存的产品列表，按当前库存升序排列。
     *
     * @return 低库存产品列表
     */
    @Transactional(readOnly = true)
    public List<StockSummaryResponse> getLowStockProducts() {
        List<Product> activeProducts = productRepository.findByActiveTrue();
        Map<Long, Long> stockMap = loadStockMap(
            activeProducts.stream().map(Product::getId).toList()
        );

        return activeProducts.stream()
            .map(p -> toStockSummary(p, stockMap.getOrDefault(p.getId(), 0L)))
            .filter(StockSummaryResponse::belowSafetyStock)
            .sorted(Comparator.comparingLong(StockSummaryResponse::currentStock))
            .toList();
    }

    /**
     * 返回指定天数内即将到期且仍有剩余库存的批次列表，按到期日升序排列。
     *
     * @param withinDays 未来天数阈值
     * @return 临期批次列表
     */
    @Transactional(readOnly = true)
    public List<BatchResponse> getExpiringBatches(int withinDays) {
        LocalDate threshold = LocalDate.now().plusDays(withinDays);
        List<Batch> batches = batchRepository.findExpiringBatches(threshold);

        Map<Long, Product> productMap = loadProductMap(
            batches.stream().map(Batch::getProductId).distinct().toList()
        );

        return batches.stream()
            .map(b -> toBatchResponse(b, productMap.get(b.getProductId())))
            .toList();
    }

    private Map<Long, Long> loadStockMap(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findCurrentStockByProductIds(productIds).stream()
            .collect(Collectors.toMap(
                ProductStockView::getProductId,
                v -> v.getCurrentStock() == null ? 0L : v.getCurrentStock()
            ));
    }

    private Map<Long, Product> loadProductMap(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
    }

    private Map<Long, Batch> loadBatchMap(Collection<Long> batchIds) {
        if (batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchRepository.findAllById(batchIds).stream()
            .collect(Collectors.toMap(Batch::getId, b -> b));
    }

    private StockSummaryResponse toStockSummary(Product product, long currentStock) {
        return new StockSummaryResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getUnit(),
            product.getSafetyStock(),
            currentStock,
            currentStock < product.getSafetyStock()
        );
    }

    private BatchResponse toBatchResponse(Batch batch, Product product) {
        return new BatchResponse(
            batch.getId(),
            batch.getProductId(),
            product != null ? product.getSku() : null,
            product != null ? product.getName() : null,
            batch.getBatchNumber(),
            batch.getArrivalDate(),
            batch.getExpiryDate(),
            batch.getQuantityReceived(),
            batch.getQuantityRemaining(),
            batch.getPurchaseUnitPrice(),
            batch.getExpiryStatus(),
            batch.getCreatedAt()
        );
    }

    private InventoryTransactionResponse toTransactionResponse(
        InventoryTransaction tx, Batch batch, Product product
    ) {
        return new InventoryTransactionResponse(
            tx.getId(),
            tx.getBatchId(),
            batch != null ? batch.getBatchNumber() : null,
            tx.getProductId(),
            product != null ? product.getSku() : null,
            product != null ? product.getName() : null,
            tx.getTransactionType(),
            tx.getQuantityDelta(),
            tx.getQuantityBefore(),
            tx.getQuantityAfter(),
            tx.getRelatedDocumentType(),
            tx.getRelatedDocumentId(),
            tx.getNote(),
            tx.getCreatedAt(),
            tx.getCreatedBy()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toLikePattern(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }

    private String toLowerCaseValue(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
