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
import com.panto.wms.order.dto.InvoiceCustomerResponse;
import com.panto.wms.order.dto.InvoiceLineResponse;
import com.panto.wms.order.dto.InvoiceResponse;
import com.panto.wms.order.dto.OrderItemResponse;
import com.panto.wms.order.dto.OrderPageResponse;
import com.panto.wms.order.dto.OrderResponse;
import com.panto.wms.order.dto.OrderSummaryResponse;
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
import java.time.ZoneOffset;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private static final String PAYMENT_INSTRUCTIONS = "Bank transfer";

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

    /**
     * 分页查询订单列表。
     *
     * @param customerId 客户 ID，可为空
     * @param dateFrom 起始日期，可为空
     * @param dateTo 结束日期，可为空
     * @param status 订单状态，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 订单分页结果
     */
    @Transactional(readOnly = true)
    public OrderPageResponse listOrders(
        Long customerId,
        LocalDate dateFrom,
        LocalDate dateTo,
        OrderStatus status,
        int page,
        int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.search(
            customerId,
            toStartOfDay(dateFrom),
            toStartOfNextDay(dateTo),
            status,
            pageRequest
        );

        List<Long> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<Long, Customer> customerMap = loadCustomerMap(
            orders.getContent().stream().map(Order::getCustomerId).distinct().toList()
        );
        Map<Long, Long> itemCountMap = loadOrderItemCountMap(orderIds);

        List<OrderSummaryResponse> items = orders.getContent().stream()
            .map(order -> toSummaryResponse(
                order,
                customerMap.get(order.getCustomerId()),
                itemCountMap.getOrDefault(order.getId(), 0L).intValue()
            ))
            .toList();

        return new OrderPageResponse(
            items,
            orders.getNumber(),
            orders.getSize(),
            orders.getTotalElements(),
            orders.getTotalPages()
        );
    }

    /**
     * 查询订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单详情
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        Customer customer = findCustomerOrThrow(order.getCustomerId());
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        Map<Long, Batch> batchMap = loadBatchMap(
            items.stream().map(OrderItem::getBatchId).distinct().toList()
        );
        return toOrderResponse(order, customer, items, batchMap);
    }

    /**
     * 回滚订单并返还原批次库存。
     *
     * @param orderId 订单 ID
     * @param reason 回滚原因
     * @param operatorId 当前操作人 ID
     * @return 回滚后的订单详情
     */
    @Transactional
    public OrderResponse rollbackOrder(Long orderId, String reason, Long operatorId) {
        try {
            return doRollbackOrder(orderId, reason, operatorId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("订单回滚时发生库存并发冲突, orderId={}, operatorId={}", orderId, operatorId, ex);
            throw new BusinessException(ErrorCode.ORDER_STOCK_CONFLICT);
        }
    }

    /**
     * 查询订单发票数据。
     *
     * @param orderId 订单 ID
     * @return 发票响应数据
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        Customer customer = findCustomerOrThrow(order.getCustomerId());
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);

        return new InvoiceResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCreatedAt(),
            order.getStatus(),
            new InvoiceCustomerResponse(
                customer.getCompanyName(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getGstNumber()
            ),
            toInvoiceLines(items),
            order.getSubtotalAmount(),
            order.getGstAmount(),
            order.getTotalAmount(),
            order.getRemarks(),
            PAYMENT_INSTRUCTIONS
        );
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

    private OrderResponse doRollbackOrder(Long orderId, String reason, Long operatorId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() == OrderStatus.ROLLED_BACK) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_ROLLED_BACK);
        }

        Customer customer = findCustomerOrThrow(order.getCustomerId());
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        Map<Long, Batch> batchMap = loadBatchMap(
            items.stream().map(OrderItem::getBatchId).distinct().toList()
        );

        OffsetDateTime now = OffsetDateTime.now();
        for (OrderItem item : items) {
            Batch batch = batchMap.get(item.getBatchId());
            if (batch == null) {
                throw new IllegalStateException("Missing batch for order item " + item.getId());
            }

            int quantityBefore = batch.getQuantityRemaining();
            int quantityAfter = quantityBefore + item.getQuantity();

            batch.setQuantityRemaining(quantityAfter);
            batch.setUpdatedAt(now);
            batch.setUpdatedBy(operatorId);
            batchRepository.save(batch);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setBatchId(batch.getId());
            tx.setProductId(item.getProductId());
            tx.setTransactionType(TransactionType.ROLLBACK);
            tx.setQuantityDelta(item.getQuantity());
            tx.setQuantityBefore(quantityBefore);
            tx.setQuantityAfter(quantityAfter);
            tx.setRelatedDocumentType(ORDER_DOCUMENT_TYPE);
            tx.setRelatedDocumentId(order.getId());
            tx.setNote("Rollback order #" + order.getOrderNumber());
            tx.setCreatedAt(now);
            tx.setCreatedBy(operatorId);
            inventoryTransactionRepository.save(tx);
        }

        order.setStatus(OrderStatus.ROLLED_BACK);
        order.setRolledBackAt(now);
        order.setRolledBackBy(operatorId);
        order.setRollbackReason(reason);
        order.setUpdatedAt(now);
        order.setUpdatedBy(operatorId);
        Order savedOrder = orderRepository.save(order);

        batchRepository.flush();

        return toOrderResponse(savedOrder, customer, items, batchMap);
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

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Customer findActiveCustomerOrThrow(Long customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty() || !Boolean.TRUE.equals(customer.get().getActive())) {
            throw new BusinessException(ErrorCode.ORDER_CUSTOMER_NOT_FOUND);
        }
        return customer.get();
    }

    private Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_CUSTOMER_NOT_FOUND));
    }

    private Map<Long, Product> loadProductMap(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Customer> loadCustomerMap(Collection<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerRepository.findAllById(customerIds).stream()
            .collect(Collectors.toMap(Customer::getId, customer -> customer, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Batch> loadBatchMap(Collection<Long> batchIds) {
        if (batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return batchRepository.findAllById(batchIds).stream()
            .collect(Collectors.toMap(Batch::getId, batch -> batch, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Long> loadOrderItemCountMap(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemRepository.findByOrderIdIn(orderIds).stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.counting()));
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

    private OrderResponse toOrderResponse(
        Order order,
        Customer customer,
        List<OrderItem> items,
        Map<Long, Batch> batchMap
    ) {
        List<OrderItemResponse> itemResponses = items.stream()
            .map(item -> toOrderItemResponse(item, batchMap.get(item.getBatchId())))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerId(),
            customer.getCompanyName(),
            order.getStatus(),
            order.getSubtotalAmount(),
            order.getGstAmount(),
            order.getTotalAmount(),
            order.getRemarks(),
            itemResponses,
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getCreatedBy(),
            order.getUpdatedBy()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item, Batch batch) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getBatchId(),
            batch != null ? batch.getBatchNumber() : null,
            batch != null ? batch.getExpiryDate() : null,
            batch != null ? batch.getExpiryStatus() : null,
            item.getProductSkuSnapshot(),
            item.getProductNameSnapshot(),
            item.getProductUnitSnapshot(),
            item.getProductSpecSnapshot(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getSubtotal(),
            item.getGstApplicable(),
            item.getGstAmount()
        );
    }

    private OrderSummaryResponse toSummaryResponse(Order order, Customer customer, int itemCount) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerId(),
            customer != null ? customer.getCompanyName() : null,
            order.getStatus(),
            itemCount,
            order.getSubtotalAmount(),
            order.getGstAmount(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            order.getCreatedBy()
        );
    }

    private List<InvoiceLineResponse> toInvoiceLines(List<OrderItem> orderItems) {
        Map<InvoiceLineKey, InvoiceLineAccumulator> aggregated = new LinkedHashMap<>();

        for (OrderItem item : orderItems) {
            InvoiceLineKey key = new InvoiceLineKey(
                item.getProductSkuSnapshot(),
                item.getProductNameSnapshot(),
                item.getProductSpecSnapshot(),
                item.getProductUnitSnapshot(),
                item.getUnitPrice(),
                item.getGstApplicable()
            );
            InvoiceLineAccumulator accumulator = aggregated.computeIfAbsent(
                key,
                ignored -> new InvoiceLineAccumulator()
            );
            accumulator.quantity += item.getQuantity();
            accumulator.subtotal = accumulator.subtotal.add(item.getSubtotal());
            accumulator.gstAmount = accumulator.gstAmount.add(item.getGstAmount());
        }

        return aggregated.entrySet().stream()
            .map(entry -> new InvoiceLineResponse(
                entry.getKey().productSku(),
                entry.getKey().productName(),
                entry.getKey().productSpecification(),
                entry.getKey().productUnit(),
                entry.getValue().quantity,
                entry.getKey().unitPrice(),
                entry.getValue().subtotal,
                entry.getKey().gstApplicable(),
                entry.getValue().gstAmount
            ))
            .toList();
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

    private record InvoiceLineKey(
        String productSku,
        String productName,
        String productSpecification,
        String productUnit,
        BigDecimal unitPrice,
        Boolean gstApplicable
    ) {
    }

    private static final class InvoiceLineAccumulator {
        private int quantity;
        private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal gstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
