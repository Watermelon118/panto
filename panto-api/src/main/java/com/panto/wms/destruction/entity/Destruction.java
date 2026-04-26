package com.panto.wms.destruction.entity;

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
 * 销毁记录实体，对应 destructions 表。
 */
@Entity
@Getter
@Setter
@Table(name = "destructions")
public class Destruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destruction_number", nullable = false, length = 30)
    private String destructionNumber;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "inventory_transaction_id", nullable = false)
    private Long inventoryTransactionId;

    @Column(name = "quantity_destroyed", nullable = false)
    private Integer quantityDestroyed;

    @Column(name = "purchase_unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchaseUnitPriceSnapshot;

    @Column(name = "loss_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal lossAmount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}
