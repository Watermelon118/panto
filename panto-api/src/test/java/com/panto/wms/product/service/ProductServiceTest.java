package com.panto.wms.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.product.dto.CreateProductRequest;
import com.panto.wms.product.dto.ProductResponse;
import com.panto.wms.product.dto.UpdateProductRequest;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import com.panto.wms.product.repository.ProductStockView;
import java.math.BigDecimal;
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
 * 产品服务测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Test
    void createProductShouldSaveProductWhenRequestIsValid() {
        CreateProductRequest request = buildCreateRequest();
        Product savedProduct = buildProduct(1L, "SKU-001", "Frozen Dumplings");

        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request, 7L);

        assertEquals(1L, response.id());
        assertEquals("SKU-001", response.sku());
        assertEquals("Frozen Dumplings", response.name());
        assertEquals(0L, response.currentStock());

        verify(productRepository).save(productCaptor.capture());
        Product captured = productCaptor.getValue();
        assertEquals("SKU-001", captured.getSku());
        assertEquals("Frozen Dumplings", captured.getName());
        assertEquals("Frozen Food", captured.getCategory());
        assertEquals("1kg x 10", captured.getSpecification());
        assertEquals("carton", captured.getUnit());
        assertEquals(new BigDecimal("12.50"), captured.getReferencePurchasePrice());
        assertEquals(new BigDecimal("18.80"), captured.getReferenceSalePrice());
        assertEquals(20, captured.getSafetyStock());
        assertEquals(Boolean.TRUE, captured.getGstApplicable());
        assertEquals(Boolean.TRUE, captured.getActive());
        assertEquals(7L, captured.getCreatedBy());
        assertEquals(7L, captured.getUpdatedBy());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void createProductShouldThrowWhenSkuAlreadyExists() {
        CreateProductRequest request = buildCreateRequest();
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.createProduct(request, 7L)
        );

        assertEquals(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void updateProductShouldSaveUpdatedFieldsWhenProductExists() {
        Product existingProduct = buildProduct(1L, "SKU-001", "Frozen Dumplings");
        UpdateProductRequest request = new UpdateProductRequest(
            "SKU-009",
            "Frozen Spring Rolls",
            "Snacks",
            "500g x 12",
            "box",
            new BigDecimal("9.90"),
            new BigDecimal("15.40"),
            12,
            false
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-009", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findCurrentStockByProductIds(List.of(1L))).thenReturn(List.of(stockView(1L, 36L)));

        ProductResponse response = productService.updateProduct(1L, request, 11L);

        assertEquals("SKU-009", response.sku());
        assertEquals("Frozen Spring Rolls", response.name());
        assertEquals("Snacks", response.category());
        assertEquals(36L, response.currentStock());
        assertFalse(response.gstApplicable());

        verify(productRepository).save(productCaptor.capture());
        Product captured = productCaptor.getValue();
        assertEquals("SKU-009", captured.getSku());
        assertEquals("Frozen Spring Rolls", captured.getName());
        assertEquals("Snacks", captured.getCategory());
        assertEquals("500g x 12", captured.getSpecification());
        assertEquals("box", captured.getUnit());
        assertEquals(new BigDecimal("9.90"), captured.getReferencePurchasePrice());
        assertEquals(new BigDecimal("15.40"), captured.getReferenceSalePrice());
        assertEquals(12, captured.getSafetyStock());
        assertEquals(Boolean.FALSE, captured.getGstApplicable());
        assertEquals(11L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void getProductShouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> productService.getProduct(99L)
        );

        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateProductStatusShouldPersistActiveFlag() {
        Product existingProduct = buildProduct(1L, "SKU-001", "Frozen Dumplings");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findCurrentStockByProductIds(List.of(1L))).thenReturn(List.of(stockView(1L, 8L)));

        ProductResponse response = productService.updateProductStatus(1L, false, 15L);

        assertFalse(response.active());
        assertEquals(8L, response.currentStock());

        verify(productRepository).save(productCaptor.capture());
        Product captured = productCaptor.getValue();
        assertFalse(captured.getActive());
        assertEquals(15L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void listProductsShouldReturnPagedItemsWithAggregatedStock() {
        Product product = buildProduct(1L, "SKU-001", "Frozen Dumplings");
        PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);

        when(productRepository.search(null, null, true, PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"
        )))).thenReturn(page);
        when(productRepository.findCurrentStockByProductIds(List.of(1L))).thenReturn(List.of(stockView(1L, 18L)));

        var response = productService.listProducts(null, null, true, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals(18L, response.items().getFirst().currentStock());
        assertEquals(1L, response.totalElements());
    }

    private CreateProductRequest buildCreateRequest() {
        return new CreateProductRequest(
            "SKU-001",
            "Frozen Dumplings",
            "Frozen Food",
            "1kg x 10",
            "carton",
            new BigDecimal("12.50"),
            new BigDecimal("18.80"),
            20,
            true
        );
    }

    private Product buildProduct(Long id, String sku, String name) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setCategory("Frozen Food");
        product.setSpecification("1kg x 10");
        product.setUnit("carton");
        product.setReferencePurchasePrice(new BigDecimal("12.50"));
        product.setReferenceSalePrice(new BigDecimal("18.80"));
        product.setSafetyStock(20);
        product.setGstApplicable(true);
        product.setActive(true);
        product.setCreatedAt(OffsetDateTime.now().minusDays(1));
        product.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        product.setCreatedBy(1L);
        product.setUpdatedBy(1L);
        return product;
    }

    private ProductStockView stockView(Long productId, Long currentStock) {
        return new ProductStockView() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getCurrentStock() {
                return currentStock;
            }
        };
    }
}
