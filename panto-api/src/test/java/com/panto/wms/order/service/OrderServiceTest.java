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
import com.panto.wms.order.dto.OrderResponse;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
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

    private Customer buildActiveCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("Fresh Dumplings Ltd");
        customer.setActive(true);
        customer.setCreatedAt(OffsetDateTime.now().minusDays(30));
        customer.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        customer.setCreatedBy(OPERATOR_ID);
        customer.setUpdatedBy(OPERATOR_ID);
        return customer;
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
