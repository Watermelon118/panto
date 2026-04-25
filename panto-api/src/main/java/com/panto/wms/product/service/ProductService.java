package com.panto.wms.product.service;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.product.dto.CreateProductRequest;
import com.panto.wms.product.dto.ProductPageResponse;
import com.panto.wms.product.dto.ProductResponse;
import com.panto.wms.product.dto.ProductSummaryResponse;
import com.panto.wms.product.dto.UpdateProductRequest;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import com.panto.wms.product.repository.ProductStockView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产品管理业务服务。
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 创建产品服务。
     *
     * @param productRepository 产品数据访问接口
     */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 返回分页筛选后的产品列表。
     *
     * @param keyword 名称或 SKU 关键字，可为空
     * @param category 分类筛选，可为空
     * @param active 启用状态筛选，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页产品列表
     */
    @Transactional(readOnly = true)
    public ProductPageResponse listProducts(String keyword, String category, Boolean active, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Product> products = productRepository.search(
            toLikePattern(keyword),
            toLowerCaseValue(category),
            active,
            pageRequest
        );

        Map<Long, Long> stockByProductId = loadStockMap(
            products.getContent().stream().map(Product::getId).toList()
        );

        List<ProductSummaryResponse> items = products.getContent().stream()
            .map(product -> toSummaryResponse(product, stockByProductId.getOrDefault(product.getId(), 0L)))
            .toList();

        return new ProductPageResponse(
            items,
            products.getNumber(),
            products.getSize(),
            products.getTotalElements(),
            products.getTotalPages()
        );
    }

    /**
     * 返回单个产品详情及当前聚合库存。
     *
     * @param productId 产品 ID
     * @return 产品详情
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        return toResponse(product, loadCurrentStock(productId));
    }

    /**
     * 创建产品。
     *
     * @param request 创建请求
     * @param operatorId 当前操作人 ID
     * @return 创建后的产品
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, Long operatorId) {
        String normalizedSku = normalizeRequired(request.sku(), "sku");
        validateSkuUniqueness(normalizedSku, null);

        OffsetDateTime now = OffsetDateTime.now();
        Product product = new Product();
        product.setSku(normalizedSku);
        applyEditableFields(
            product,
            request.name(),
            request.category(),
            request.specification(),
            request.unit(),
            request.referencePurchasePrice(),
            request.referenceSalePrice(),
            request.safetyStock(),
            request.gstApplicable()
        );
        product.setActive(true);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        product.setCreatedBy(operatorId);
        product.setUpdatedBy(operatorId);

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct, 0L);
    }

    /**
     * 更新产品。
     *
     * @param productId 产品 ID
     * @param request 更新请求
     * @param operatorId 当前操作人 ID
     * @return 更新后的产品
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request, Long operatorId) {
        Product product = findProductOrThrow(productId);
        String normalizedSku = normalizeRequired(request.sku(), "sku");
        validateSkuUniqueness(normalizedSku, productId);

        product.setSku(normalizedSku);
        applyEditableFields(
            product,
            request.name(),
            request.category(),
            request.specification(),
            request.unit(),
            request.referencePurchasePrice(),
            request.referenceSalePrice(),
            request.safetyStock(),
            request.gstApplicable()
        );
        product.setUpdatedAt(OffsetDateTime.now());
        product.setUpdatedBy(operatorId);

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct, loadCurrentStock(savedProduct.getId()));
    }

    /**
     * 更新产品启用状态。
     *
     * @param productId 产品 ID
     * @param active 目标启用状态
     * @param operatorId 当前操作人 ID
     * @return 更新后的产品
     */
    @Transactional
    public ProductResponse updateProductStatus(Long productId, boolean active, Long operatorId) {
        Product product = findProductOrThrow(productId);
        product.setActive(active);
        product.setUpdatedAt(OffsetDateTime.now());
        product.setUpdatedBy(operatorId);

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct, loadCurrentStock(savedProduct.getId()));
    }

    /**
     * 返回启用产品的分类列表。
     *
     * @return 分类列表
     */
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return productRepository.findDistinctActiveCategories();
    }

    /**
     * 返回启用产品的单位列表。
     *
     * @return 单位列表
     */
    @Transactional(readOnly = true)
    public List<String> listUnits() {
        return productRepository.findDistinctActiveUnits();
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateSkuUniqueness(String sku, Long productId) {
        boolean exists = productId == null
            ? productRepository.existsBySkuIgnoreCase(sku)
            : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId);

        if (exists) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
        }
    }

    private void applyEditableFields(
        Product product,
        String name,
        String category,
        String specification,
        String unit,
        BigDecimal referencePurchasePrice,
        BigDecimal referenceSalePrice,
        Integer safetyStock,
        Boolean gstApplicable
    ) {
        product.setName(normalizeRequired(name, "name"));
        product.setCategory(normalizeRequired(category, "category"));
        product.setSpecification(normalize(specification));
        product.setUnit(normalizeRequired(unit, "unit"));
        product.setReferencePurchasePrice(referencePurchasePrice);
        product.setReferenceSalePrice(referenceSalePrice);
        product.setSafetyStock(safetyStock);
        product.setGstApplicable(gstApplicable);
    }

    private Map<Long, Long> loadStockMap(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return productRepository.findCurrentStockByProductIds(productIds).stream()
            .collect(Collectors.toMap(ProductStockView::getProductId, stock -> defaultStock(stock.getCurrentStock())));
    }

    private long loadCurrentStock(Long productId) {
        return loadStockMap(List.of(productId)).getOrDefault(productId, 0L);
    }

    private ProductSummaryResponse toSummaryResponse(Product product, long currentStock) {
        return new ProductSummaryResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getUnit(),
            product.getReferencePurchasePrice(),
            product.getReferenceSalePrice(),
            product.getSafetyStock(),
            product.getGstApplicable(),
            product.getActive(),
            currentStock
        );
    }

    private ProductResponse toResponse(Product product, long currentStock) {
        return new ProductResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getSpecification(),
            product.getUnit(),
            product.getReferencePurchasePrice(),
            product.getReferenceSalePrice(),
            product.getSafetyStock(),
            product.getGstApplicable(),
            product.getActive(),
            currentStock,
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getCreatedBy(),
            product.getUpdatedBy()
        );
    }

    private long defaultStock(Long currentStock) {
        return currentStock == null ? 0L : currentStock;
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

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " must not be blank");
        }
        return normalized;
    }
}
