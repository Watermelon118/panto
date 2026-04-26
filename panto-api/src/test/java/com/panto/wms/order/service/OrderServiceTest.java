package com.panto.wms.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.customer.entity.Customer;
import com.panto.wms.customer.repository.CustomerRepository;
import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.domain.TransactionType;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.entity.InventoryTransaction;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.inventory.repository.InventoryTransactionRepository;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.dto.CreateOrderItemRequest;
import com.panto.wms.order.dto.CreateOrderRequest;
import com.panto.wms.order.dto.InvoiceResponse;
import com.panto.wms.order.dto.OrderPageResponse;
import com.panto.wms.order.dto.OrderResponse;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import com.panto.wms.settings.service.SystemSettingService;
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
 * 销售订单服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long OPERATOR_ID = 1L;
    private static final Long CUSTOMER_ID = 2L;
    private static final Long PRODUCT_ID = 10L;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private SystemSettingService systemSettingService;
    @Mock private InvoicePdfService invoicePdfService;

    @InjectMocks
    private OrderService orderService;

    @Captor private ArgumentCaptor<Order> orderCaptor;
    @Captor private ArgumentCaptor<OrderItem> orderItemCaptor;
    @Captor private ArgumentCaptor<InventoryTransaction> transactionCaptor;

    @Test
    void createOrderShouldSplitAcrossBatchesByFifoAndPersistTransactions() {
        CreateOrderRequest request = new CreateOrderRequest(
            CUSTOMER_ID,
            "Deliver before noon",
            List.of(new CreateOrderItemRequest(PRODUCT_ID, 12, new BigDecimal("20.00")))
        );
        Customer customer = buildActiveCustomer();
        Product product = buildActiveProduct(true);
        Batch firstBatch = buildBatch(100L, 5, LocalDate.now().plusDays(2), ExpiryStatus.EXPIRING_SOON);
        Batch secondBatch = buildBatch(101L, 10, LocalDate.now().plusDays(8), ExpiryStatus.NORMAL);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(batchRepository.findAvailableByProductIdOrderByExpiryDateAsc(PRODUCT_ID))
            .thenReturn(List.of(firstBatch, secondBatch));
        when(orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(500L);
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(item.getBatchId() + 1000);
            return item;
        });
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request, OPERATOR_ID);

        assertEquals(OrderStatus.ACTIVE, response.status());
        assertEquals(CUSTOMER_ID, response.customerId());
        assertEquals("Fresh Dumplings Ltd", response.customerCompanyName());
        assertEquals(new BigDecimal("240.00"), response.subtotalAmount());
        assertEquals(new BigDecimal("36.00"), response.gstAmount());
        assertEquals(new BigDecimal("276.00"), response.totalAmount());
        assertEquals(2, response.items().size());
        assertEquals(5, response.items().get(0).quantity());
        assertEquals(7, response.items().get(1).quantity());
        assertEquals("BATCH-001", response.items().get(0).batchNumber());
        assertEquals("BATCH-002", response.items().get(1).batchNumber());
        assertNotNull(response.orderNumber());

        assertEquals(0, firstBatch.getQuantityRemaining());
        assertEquals(3, secondBatch.getQuantityRemaining());

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.ACTIVE, savedOrder.getStatus());
        assertEquals(new BigDecimal("240.00"), savedOrder.getSubtotalAmount());
        assertEquals(new BigDecimal("36.00"), savedOrder.getGstAmount());
        assertEquals(new BigDecimal("276.00"), savedOrder.getTotalAmount());
        assertEquals("Deliver before noon", savedOrder.getRemarks());

        verify(orderItemRepository, times(2)).save(orderItemCaptor.capture());
        List<OrderItem> savedItems = orderItemCaptor.getAllValues();
        assertEquals(5, savedItems.get(0).getQuantity());
        assertEquals(7, savedItems.get(1).getQuantity());
        assertEquals(new BigDecimal("100.00"), savedItems.get(0).getSubtotal());
        assertEquals(new BigDecimal("140.00"), savedItems.get(1).getSubtotal());

        verify(inventoryTransactionRepository, times(2)).save(transactionCaptor.capture());
        List<InventoryTransaction> transactions = transactionCaptor.getAllValues();
        assertEquals(TransactionType.OUT, transactions.get(0).getTransactionType());
        assertEquals(-5, transactions.get(0).getQuantityDelta());
        assertEquals(5, transactions.get(0).getQuantityBefore());
        assertEquals(0, transactions.get(0).getQuantityAfter());
        assertEquals("ORDER", transactions.get(0).getRelatedDocumentType());
        assertEquals(-7, transactions.get(1).getQuantityDelta());
        assertEquals(10, transactions.get(1).getQuantityBefore());
        assertEquals(3, transactions.get(1).getQuantityAfter());

        verify(batchRepository).flush();
    }

    @Test
    void createOrderShouldUseReferenceSalePriceWhenUnitPriceIsNotProvided() {
        CreateOrderRequest request = new CreateOrderRequest(
            CUSTOMER_ID,
            null,
            List.of(new CreateOrderItemRequest(PRODUCT_ID, 2, null))
        );
        Customer customer = buildActiveCustomer();
        Product product = buildActiveProduct(false);
        Batch batch = buildBatch(100L, 10, LocalDate.now().plusDays(10), ExpiryStatus.NORMAL);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(batchRepository.findAvailableByProductIdOrderByExpiryDateAsc(PRODUCT_ID)).thenReturn(List.of(batch));
        when(orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(501L);
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(2000L);
            return item;
        });
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request, OPERATOR_ID);

        assertEquals(new BigDecimal("37.60"), response.subtotalAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), response.gstAmount());
        assertEquals(new BigDecimal("37.60"), response.totalAmount());
        assertEquals(new BigDecimal("18.80"), response.items().getFirst().unitPrice());
    }

    @Test
    void createOrderShouldThrowWhenCustomerIsInactive() {
        CreateOrderRequest request = new CreateOrderRequest(
            CUSTOMER_ID,
            null,
            List.of(new CreateOrderItemRequest(PRODUCT_ID, 1, new BigDecimal("10.00")))
        );
        Customer customer = buildActiveCustomer();
        customer.setActive(false);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> orderService.createOrder(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.ORDER_CUSTOMER_NOT_FOUND, ex.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderShouldThrowWhenProductIsDuplicated() {
        CreateOrderRequest request = new CreateOrderRequest(
            CUSTOMER_ID,
            null,
            List.of(
                new CreateOrderItemRequest(PRODUCT_ID, 1, new BigDecimal("10.00")),
                new CreateOrderItemRequest(PRODUCT_ID, 2, new BigDecimal("10.00"))
            )
        );
        Customer customer = buildActiveCustomer();
        Product product = buildActiveProduct(true);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> orderService.createOrder(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.ORDER_DUPLICATE_PRODUCT, ex.getErrorCode());
        verify(batchRepository, never()).findAvailableByProductIdOrderByExpiryDateAsc(any());
    }

    @Test
    void createOrderShouldThrowWhenStockIsInsufficient() {
        CreateOrderRequest request = new CreateOrderRequest(
            CUSTOMER_ID,
            null,
            List.of(new CreateOrderItemRequest(PRODUCT_ID, 20, new BigDecimal("12.00")))
        );
        Customer customer = buildActiveCustomer();
        Product product = buildActiveProduct(true);
        Batch batch = buildBatch(100L, 6, LocalDate.now().plusDays(3), ExpiryStatus.NORMAL);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(batchRepository.findAvailableByProductIdOrderByExpiryDateAsc(PRODUCT_ID)).thenReturn(List.of(batch));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> orderService.createOrder(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.ORDER_INSUFFICIENT_STOCK, ex.getErrorCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderShouldTranslateOptimisticLockException() {
        CreateOrderRequest request = new CreateOrderRequest(
            CUSTOMER_ID,
            null,
            List.of(new CreateOrderItemRequest(PRODUCT_ID, 2, new BigDecimal("10.00")))
        );
        Customer customer = buildActiveCustomer();
        Product product = buildActiveProduct(true);
        Batch batch = buildBatch(100L, 4, LocalDate.now().plusDays(5), ExpiryStatus.NORMAL);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(batchRepository.findAvailableByProductIdOrderByExpiryDateAsc(PRODUCT_ID)).thenReturn(List.of(batch));
        when(orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(502L);
            return order;
        });
        when(batchRepository.save(any(Batch.class)))
            .thenThrow(new ObjectOptimisticLockingFailureException(Batch.class, 100L));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> orderService.createOrder(request, OPERATOR_ID)
        );

        assertEquals(ErrorCode.ORDER_STOCK_CONFLICT, ex.getErrorCode());
    }

    @Test
    void listOrdersShouldReturnPagedSummariesWithCustomerAndItemCounts() {
        Order order = buildOrder(700L, "ORD-20260425-001", OrderStatus.ACTIVE);
        PageImpl<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);

        when(orderRepository.search(null, null, null, null, PageRequest.of(0, 20))).thenReturn(page);
        when(customerRepository.findAllById(List.of(CUSTOMER_ID))).thenReturn(List.of(buildActiveCustomer()));
        when(orderItemRepository.findByOrderIdIn(List.of(700L))).thenReturn(List.of(
            buildOrderItem(1700L, 700L, 100L, 5, new BigDecimal("100.00"), new BigDecimal("15.00")),
            buildOrderItem(1701L, 700L, 101L, 7, new BigDecimal("140.00"), new BigDecimal("21.00"))
        ));

        OrderPageResponse response = orderService.listOrders(null, null, null, null, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals("ORD-20260425-001", response.items().getFirst().orderNumber());
        assertEquals("Fresh Dumplings Ltd", response.items().getFirst().customerCompanyName());
        assertEquals(2, response.items().getFirst().itemCount());
        assertEquals(new BigDecimal("276.00"), response.items().getFirst().totalAmount());
    }

    @Test
    void listOrdersShouldPassStatusNameToRepository() {
        PageImpl<Order> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(orderRepository.search(null, null, null, "ACTIVE", PageRequest.of(0, 20))).thenReturn(page);

        OrderPageResponse response = orderService.listOrders(null, null, null, OrderStatus.ACTIVE, 0, 20);

        assertEquals(0, response.items().size());
        verify(orderRepository).search(null, null, null, "ACTIVE", PageRequest.of(0, 20));
    }

    @Test
    void getOrderShouldReturnDetailWithBatchInfo() {
        Order order = buildOrder(701L, "ORD-20260425-002", OrderStatus.ACTIVE);
        OrderItem firstItem = buildOrderItem(1800L, 701L, 100L, 5, new BigDecimal("100.00"), new BigDecimal("15.00"));
        OrderItem secondItem = buildOrderItem(1801L, 701L, 101L, 7, new BigDecimal("140.00"), new BigDecimal("21.00"));
        Batch firstBatch = buildBatch(100L, 5, LocalDate.now().plusDays(2), ExpiryStatus.EXPIRING_SOON);
        Batch secondBatch = buildBatch(101L, 7, LocalDate.now().plusDays(6), ExpiryStatus.NORMAL);

        when(orderRepository.findById(701L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(buildActiveCustomer()));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(701L)).thenReturn(List.of(firstItem, secondItem));
        when(batchRepository.findAllById(List.of(100L, 101L))).thenReturn(List.of(firstBatch, secondBatch));

        OrderResponse response = orderService.getOrder(701L);

        assertEquals("ORD-20260425-002", response.orderNumber());
        assertEquals(OrderStatus.ACTIVE, response.status());
        assertEquals(2, response.items().size());
        assertEquals("BATCH-001", response.items().getFirst().batchNumber());
        assertEquals(ExpiryStatus.EXPIRING_SOON, response.items().getFirst().batchExpiryStatus());
        assertEquals("BATCH-002", response.items().get(1).batchNumber());
    }

    @Test
    void rollbackOrderShouldRestoreStockAndPersistRollbackTransactions() {
        Order order = buildOrder(702L, "ORD-20260425-003", OrderStatus.ACTIVE);
        OrderItem firstItem = buildOrderItem(1900L, 702L, 100L, 5, new BigDecimal("100.00"), new BigDecimal("15.00"));
        OrderItem secondItem = buildOrderItem(1901L, 702L, 101L, 7, new BigDecimal("140.00"), new BigDecimal("21.00"));
        Batch firstBatch = buildBatch(100L, 0, LocalDate.now().plusDays(2), ExpiryStatus.EXPIRING_SOON);
        Batch secondBatch = buildBatch(101L, 3, LocalDate.now().plusDays(6), ExpiryStatus.NORMAL);

        when(orderRepository.findById(702L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(buildActiveCustomer()));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(702L)).thenReturn(List.of(firstItem, secondItem));
        when(batchRepository.findAllById(List.of(100L, 101L))).thenReturn(List.of(firstBatch, secondBatch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.rollbackOrder(702L, "Customer cancelled", OPERATOR_ID);

        assertEquals(OrderStatus.ROLLED_BACK, response.status());
        assertEquals(5, firstBatch.getQuantityRemaining());
        assertEquals(10, secondBatch.getQuantityRemaining());

        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.ROLLED_BACK, savedOrder.getStatus());
        assertEquals("Customer cancelled", savedOrder.getRollbackReason());
        assertEquals(OPERATOR_ID, savedOrder.getRolledBackBy());
        assertNotNull(savedOrder.getRolledBackAt());

        verify(inventoryTransactionRepository, times(2)).save(transactionCaptor.capture());
        List<InventoryTransaction> rollbackTransactions = transactionCaptor.getAllValues();
        assertEquals(TransactionType.ROLLBACK, rollbackTransactions.get(0).getTransactionType());
        assertEquals(5, rollbackTransactions.get(0).getQuantityDelta());
        assertEquals(0, rollbackTransactions.get(0).getQuantityBefore());
        assertEquals(5, rollbackTransactions.get(0).getQuantityAfter());
        assertEquals(7, rollbackTransactions.get(1).getQuantityDelta());
        assertEquals(3, rollbackTransactions.get(1).getQuantityBefore());
        assertEquals(10, rollbackTransactions.get(1).getQuantityAfter());

        verify(batchRepository).flush();
    }

    @Test
    void rollbackOrderShouldThrowWhenOrderAlreadyRolledBack() {
        Order order = buildOrder(703L, "ORD-20260425-004", OrderStatus.ROLLED_BACK);
        when(orderRepository.findById(703L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> orderService.rollbackOrder(703L, "Duplicate request", OPERATOR_ID)
        );

        assertEquals(ErrorCode.ORDER_ALREADY_ROLLED_BACK, ex.getErrorCode());
        verify(orderItemRepository, never()).findByOrderIdOrderByIdAsc(any());
    }

    @Test
    void getInvoiceShouldAggregateOrderItemsByProductSnapshot() {
        Order order = buildOrder(704L, "ORD-20260425-005", OrderStatus.ACTIVE);
        OrderItem firstItem = buildOrderItem(2000L, 704L, 100L, 5, new BigDecimal("100.00"), new BigDecimal("15.00"));
        OrderItem secondItem = buildOrderItem(2001L, 704L, 101L, 7, new BigDecimal("140.00"), new BigDecimal("21.00"));

        when(orderRepository.findById(704L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(buildActiveCustomer()));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(704L)).thenReturn(List.of(firstItem, secondItem));
        when(systemSettingService.getInvoiceSellerCompanyName()).thenReturn("Panto Trading Ltd");
        when(systemSettingService.getInvoiceSellerGstNumber()).thenReturn("GST-9988");
        when(systemSettingService.getInvoiceSellerAddress()).thenReturn("1 Queen Street, Auckland");
        when(systemSettingService.getInvoiceSellerPhone()).thenReturn("09 123 4567");
        when(systemSettingService.getInvoiceSellerEmail()).thenReturn("accounts@panto.co.nz");
        when(systemSettingService.getInvoicePaymentInstructions()).thenReturn("Bank transfer");

        InvoiceResponse response = orderService.getInvoice(704L);

        assertEquals("ORD-20260425-005", response.invoiceNumber());
        assertEquals(OrderStatus.ACTIVE, response.status());
        assertEquals("Panto Trading Ltd", response.seller().companyName());
        assertEquals("GST-9988", response.seller().gstNumber());
        assertEquals("Fresh Dumplings Ltd", response.customer().companyName());
        assertEquals(1, response.items().size());
        assertEquals(12, response.items().getFirst().quantity());
        assertEquals(new BigDecimal("240.00"), response.items().getFirst().subtotal());
        assertEquals(new BigDecimal("36.00"), response.items().getFirst().gstAmount());
        assertEquals("Bank transfer", response.paymentInstructions());
    }

    @Test
    void getOrderShouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> orderService.getOrder(999L)
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    private Customer buildActiveCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("Fresh Dumplings Ltd");
        customer.setContactPerson("Alex Chen");
        customer.setPhone("021888999");
        customer.setAddress("99 Queen Street");
        customer.setGstNumber("GST-7788");
        customer.setActive(true);
        customer.setCreatedAt(OffsetDateTime.now().minusDays(30));
        customer.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        customer.setCreatedBy(OPERATOR_ID);
        customer.setUpdatedBy(OPERATOR_ID);
        return customer;
    }

    private Order buildOrder(Long id, String orderNumber, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setCustomerId(CUSTOMER_ID);
        order.setStatus(status);
        order.setSubtotalAmount(new BigDecimal("240.00"));
        order.setGstAmount(new BigDecimal("36.00"));
        order.setTotalAmount(new BigDecimal("276.00"));
        order.setRemarks("Deliver before noon");
        order.setCreatedAt(OffsetDateTime.now().minusDays(1));
        order.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        order.setCreatedBy(OPERATOR_ID);
        order.setUpdatedBy(OPERATOR_ID);
        return order;
    }

    private OrderItem buildOrderItem(
        Long id,
        Long orderId,
        Long batchId,
        int quantity,
        BigDecimal subtotal,
        BigDecimal gstAmount
    ) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setOrderId(orderId);
        orderItem.setProductId(PRODUCT_ID);
        orderItem.setBatchId(batchId);
        orderItem.setProductSkuSnapshot("DUMP001");
        orderItem.setProductNameSnapshot("Frozen Dumplings");
        orderItem.setProductUnitSnapshot("carton");
        orderItem.setProductSpecSnapshot("1kg x 10");
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(new BigDecimal("20.00"));
        orderItem.setSubtotal(subtotal);
        orderItem.setGstApplicable(true);
        orderItem.setGstAmount(gstAmount);
        orderItem.setCreatedAt(OffsetDateTime.now().minusDays(1));
        return orderItem;
    }

    private Product buildActiveProduct(boolean gstApplicable) {
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
        product.setGstApplicable(gstApplicable);
        product.setActive(true);
        product.setCreatedAt(OffsetDateTime.now().minusDays(30));
        product.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        product.setCreatedBy(OPERATOR_ID);
        product.setUpdatedBy(OPERATOR_ID);
        return product;
    }

    private Batch buildBatch(Long id, int quantityRemaining, LocalDate expiryDate, ExpiryStatus expiryStatus) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProductId(PRODUCT_ID);
        batch.setInboundItemId(id + 500);
        batch.setBatchNumber(id.equals(100L) ? "BATCH-001" : "BATCH-002");
        batch.setArrivalDate(LocalDate.now().minusDays(2));
        batch.setExpiryDate(expiryDate);
        batch.setQuantityReceived(quantityRemaining);
        batch.setQuantityRemaining(quantityRemaining);
        batch.setPurchaseUnitPrice(new BigDecimal("12.50"));
        batch.setExpiryStatus(expiryStatus);
        batch.setVersion(0);
        batch.setCreatedAt(OffsetDateTime.now().minusDays(3));
        batch.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        batch.setCreatedBy(OPERATOR_ID);
        batch.setUpdatedBy(OPERATOR_ID);
        return batch;
    }
}
