package com.panto.wms.order.service;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.customer.entity.Customer;
import com.panto.wms.customer.repository.CustomerRepository;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.entity.InventoryTransaction;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.repository.InventoryTransactionRepository;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.dto.CreateOrderItemRequest;
import com.panto.wms.order.dto.CreateOrderRequest;
import com.panto.wms.order.dto.OrderItemResponse;
import com.panto.wms.order.dto.OrderResponse;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 销售订单业务服务。
 */
@Slf4j
@Service
public class OrderService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.15");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ORDER_DOCUMENT_TYPE = "ORDER";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * 创建订单业务服务。
     *
     * @param orderRepository 订单仓储
     * @param orderItemRepository 订单明细仓储
     * @param customerRepository 客户仓储
     * @param productRepository 商品仓储
     * @param batchRepository 批次仓储
     * @param inventoryTransactionRepository 库存事务仓储
     */
    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        BatchRepository batchRepository,
        InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    /**
     * 创建销售订单，并按 FIFO 规则扣减库存批次。
     *
     * @param request 创建请求体
     * @param operatorId 当前操作人 ID
     * @return 创建后的订单详情
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long operatorId) {
        try {
            return doCreateOrder(request, operatorId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("订单创建时发生库存并发冲突, customerId={}, operatorId={}", request.customerId(), operatorId, ex);
            throw new BusinessException(ErrorCode.ORDER_STOCK_CONFLICT);
        }
    }

    private OrderResponse doCreateOrder(CreateOrderRequest request, Long operatorId) {
        Customer customer = findActiveCustomerOrThrow(request.customerId());

        List<Long> productIds = request.items().stream()
            .map(CreateOrderItemRequest::productId)
            .distinct()
            .toList();
        Map<Long, Product> productMap = loadProductMap(productIds);
        validateProducts(request.items(), productMap);

        List<OrderItemPlan> itemPlans = request.items().stream()
            .map(item -> buildItemPlan(item, productMap.get(item.productId())))
            .toList();

        BigDecimal subtotalAmount = itemPlans.stream()
            .map(OrderItemPlan::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstAmount = itemPlans.stream()
            .map(OrderItemPlan::gstAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = subtotalAmount.add(gstAmount);

        OffsetDateTime now = OffsetDateTime.now();
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber(now));
        order.setCustomerId(customer.getId());
        order.setStatus(OrderStatus.ACTIVE);
        order.setSubtotalAmount(subtotalAmount);
        order.setGstAmount(gstAmount);
        order.setTotalAmount(totalAmount);
        order.setRemarks(request.remarks());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setCreatedBy(operatorId);
        order.setUpdatedBy(operatorId);
        Order savedOrder = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItemPlan itemPlan : itemPlans) {
            for (BatchAllocation allocation : itemPlan.allocations()) {
                itemResponses.add(persistAllocation(savedOrder, itemPlan, allocation, now, operatorId));
            }
        }

        batchRepository.flush();

        return new OrderResponse(
            savedOrder.getId(),
            savedOrder.getOrderNumber(),
            customer.getId(),
            customer.getCompanyName(),
            savedOrder.getStatus(),
            savedOrder.getSubtotalAmount(),
            savedOrder.getGstAmount(),
            savedOrder.getTotalAmount(),
            savedOrder.getRemarks(),
            itemResponses,
            savedOrder.getCreatedAt(),
            savedOrder.getUpdatedAt(),
            savedOrder.getCreatedBy(),
            savedOrder.getUpdatedBy()
        );
    }

    private OrderItemResponse persistAllocation(
        Order order,
        OrderItemPlan itemPlan,
        BatchAllocation allocation,
        OffsetDateTime now,
        Long operatorId
    ) {
        Batch batch = allocation.batch();
        int quantityBefore = batch.getQuantityRemaining();
        int quantityAfter = quantityBefore - allocation.quantity();

        batch.setQuantityRemaining(quantityAfter);
        batch.setUpdatedAt(now);
        batch.setUpdatedBy(operatorId);
        batchRepository.save(batch);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductId(itemPlan.product().getId());
        orderItem.setBatchId(batch.getId());
        orderItem.setProductNameSnapshot(itemPlan.product().getName());
        orderItem.setProductSkuSnapshot(itemPlan.product().getSku());
        orderItem.setProductUnitSnapshot(itemPlan.product().getUnit());
        orderItem.setProductSpecSnapshot(itemPlan.product().getSpecification());
        orderItem.setQuantity(allocation.quantity());
        orderItem.setUnitPrice(itemPlan.unitPrice());
        orderItem.setSubtotal(allocation.subtotal());
        orderItem.setGstApplicable(itemPlan.product().getGstApplicable());
        orderItem.setGstAmount(allocation.gstAmount());
        orderItem.setCreatedAt(now);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setBatchId(batch.getId());
        tx.setProductId(itemPlan.product().getId());
        tx.setTransactionType(TransactionType.OUT);
        tx.setQuantityDelta(-allocation.quantity());
        tx.setQuantityBefore(quantityBefore);
        tx.setQuantityAfter(quantityAfter);
        tx.setRelatedDocumentType(ORDER_DOCUMENT_TYPE);
        tx.setRelatedDocumentId(order.getId());
        tx.setNote("Order #" + order.getOrderNumber());
        tx.setCreatedAt(now);
        tx.setCreatedBy(operatorId);
        inventoryTransactionRepository.save(tx);

        return new OrderItemResponse(
            savedOrderItem.getId(),
            savedOrderItem.getProductId(),
            savedOrderItem.getBatchId(),
            batch.getBatchNumber(),
            batch.getExpiryDate(),
            batch.getExpiryStatus(),
            savedOrderItem.getProductSkuSnapshot(),
            savedOrderItem.getProductNameSnapshot(),
            savedOrderItem.getProductUnitSnapshot(),
            savedOrderItem.getProductSpecSnapshot(),
            savedOrderItem.getQuantity(),
            savedOrderItem.getUnitPrice(),
            savedOrderItem.getSubtotal(),
            savedOrderItem.getGstApplicable(),
            savedOrderItem.getGstAmount()
        );
    }

    private Customer findActiveCustomerOrThrow(Long customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty() || !Boolean.TRUE.equals(customer.get().getActive())) {
            throw new BusinessException(ErrorCode.ORDER_CUSTOMER_NOT_FOUND);
        }
        return customer.get();
    }

    private Map<Long, Product> loadProductMap(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
    }

    private void validateProducts(
        List<CreateOrderItemRequest> itemRequests,
        Map<Long, Product> productMap
    ) {
        Map<Long, Long> requestCountByProductId = itemRequests.stream()
            .collect(Collectors.groupingBy(CreateOrderItemRequest::productId, Collectors.counting()));

        boolean hasDuplicateProduct = requestCountByProductId.values().stream().anyMatch(count -> count > 1);
        if (hasDuplicateProduct) {
            throw new BusinessException(ErrorCode.ORDER_DUPLICATE_PRODUCT);
        }

        for (CreateOrderItemRequest itemRequest : itemRequests) {
            Product product = productMap.get(itemRequest.productId());
            if (product == null) {
                throw new BusinessException(ErrorCode.ORDER_PRODUCT_NOT_FOUND);
            }
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BusinessException(ErrorCode.ORDER_PRODUCT_INACTIVE);
            }
        }
    }

    private OrderItemPlan buildItemPlan(CreateOrderItemRequest itemRequest, Product product) {
        BigDecimal unitPrice = Optional.ofNullable(itemRequest.unitPrice())
            .orElse(product.getReferenceSalePrice())
            .setScale(2, RoundingMode.HALF_UP);

        List<Batch> availableBatches = batchRepository.findAvailableByProductIdOrderByExpiryDateAsc(product.getId());
        int totalAvailable = availableBatches.stream().mapToInt(Batch::getQuantityRemaining).sum();
        if (totalAvailable < itemRequest.quantity()) {
            throw new BusinessException(
                ErrorCode.ORDER_INSUFFICIENT_STOCK,
                "商品 [%s] 库存不足，当前可用库存为 %d".formatted(product.getName(), totalAvailable)
            );
        }

        int remainingToAllocate = itemRequest.quantity();
        List<BatchAllocation> allocations = new ArrayList<>();
        for (Batch batch : availableBatches) {
            if (remainingToAllocate == 0) {
                break;
            }

            int allocatedQuantity = Math.min(batch.getQuantityRemaining(), remainingToAllocate);
            if (allocatedQuantity <= 0) {
                continue;
            }

            BigDecimal subtotal = calculateAmount(unitPrice, allocatedQuantity);
            BigDecimal gstAmount = Boolean.TRUE.equals(product.getGstApplicable())
                ? calculateGst(subtotal)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            allocations.add(new BatchAllocation(batch, allocatedQuantity, subtotal, gstAmount));
            remainingToAllocate -= allocatedQuantity;
        }

        BigDecimal subtotal = allocations.stream()
            .map(BatchAllocation::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstAmount = allocations.stream()
            .map(BatchAllocation::gstAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderItemPlan(product, unitPrice, allocations, subtotal, gstAmount);
    }

    private String generateOrderNumber(OffsetDateTime now) {
        LocalDate localDate = now.toLocalDate();
        OffsetDateTime start = localDate.atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime end = start.plusDays(1);
        long count = orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        return String.format("ORD-%s-%03d", localDate.format(DATE_FORMAT), count + 1);
    }

    private BigDecimal calculateAmount(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGst(BigDecimal subtotal) {
        return subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private record OrderItemPlan(
        Product product,
        BigDecimal unitPrice,
        List<BatchAllocation> allocations,
        BigDecimal subtotal,
        BigDecimal gstAmount
    ) {
    }

    private record BatchAllocation(
        Batch batch,
        int quantity,
        BigDecimal subtotal,
        BigDecimal gstAmount
    ) {
    }
}
