package com.panto.wms.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * products 表对应的产品实体。
 */
@Entity
@Getter
@Setter
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 100)
    private String specification;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "reference_purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal referencePurchasePrice;

    @Column(name = "reference_sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal referenceSalePrice;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(name = "is_gst_applicable", nullable = false)
    private Boolean gstApplicable;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;
}
