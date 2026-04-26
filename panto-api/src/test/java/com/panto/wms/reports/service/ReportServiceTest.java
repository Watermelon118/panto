package com.panto.wms.reports.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.panto.wms.auth.entity.User;
import com.panto.wms.auth.repository.UserRepository;
import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.customer.entity.Customer;
import com.panto.wms.customer.repository.CustomerRepository;
import com.panto.wms.destruction.entity.Destruction;
import com.panto.wms.destruction.repository.DestructionRepository;
import com.panto.wms.inventory.domain.ExpiryStatus;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 报表导出业务服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private DestructionRepository destructionRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void exportSalesShouldGenerateWorkbookWithExpectedContent() throws Exception {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        Order order = buildOrder();
        OrderItem item = buildOrderItem();
        Customer customer = buildCustomer();
        User operator = buildOperator();

        when(orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(OrderStatus.ACTIVE), any(), any()))
            .thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIdIn(List.of(10L))).thenReturn(List.of(item));
        when(customerRepository.findAllById(List.of(1L))).thenReturn(List.of(customer));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(operator));

        ReportService.ReportFile reportFile = reportService.exportSales(from, to, "xlsx");

        assertEquals("sales-report-2026-04-01-to-2026-04-30.xlsx", reportFile.fileName());
        assertTrue(reportFile.content().length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(reportFile.content()))) {
            var sheet = workbook.getSheet("Sales");
            assertEquals("Order Number", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Operator", sheet.getRow(0).getCell(10).getStringCellValue());
            assertEquals("ORD-20260426-001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Panto Trading Ltd", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("DUMP001", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals(2D, sheet.getRow(1).getCell(5).getNumericCellValue());
            assertEquals(46D, sheet.getRow(1).getCell(9).getNumericCellValue());
            assertEquals("Alice Admin (admin)", sheet.getRow(1).getCell(10).getStringCellValue());
        }
    }

    @Test
    void exportLossesShouldGenerateWorkbookWithExpectedContent() throws Exception {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        Destruction destruction = buildDestruction();
        Batch batch = buildBatch();
        Product product = buildProduct();
        User operator = buildOperator();

        when(destructionRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(any(), any()))
            .thenReturn(List.of(destruction));
        when(batchRepository.findAllById(List.of(100L))).thenReturn(List.of(batch));
        when(productRepository.findAllById(List.of(5L))).thenReturn(List.of(product));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(operator));

        ReportService.ReportFile reportFile = reportService.exportLosses(from, to, "xlsx");

        assertEquals("loss-report-2026-04-01-to-2026-04-30.xlsx", reportFile.fileName());
        assertTrue(reportFile.content().length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(reportFile.content()))) {
            var sheet = workbook.getSheet("Losses");
            assertEquals("Destruction Number", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Operator", sheet.getRow(0).getCell(8).getStringCellValue());
            assertEquals("DES-20260426-001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("DUMP001", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("BATCH-001", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals(4D, sheet.getRow(1).getCell(5).getNumericCellValue());
            assertEquals(50D, sheet.getRow(1).getCell(7).getNumericCellValue());
            assertEquals("Alice Admin (admin)", sheet.getRow(1).getCell(8).getStringCellValue());
            assertEquals("Expired stock", sheet.getRow(1).getCell(9).getStringCellValue());
        }
    }

    @Test
    void exportSalesShouldGenerateCsvWithReadableOperator() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        Order order = buildOrder();
        OrderItem item = buildOrderItem();
        Customer customer = buildCustomer();
        User operator = buildOperator();

        when(orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(OrderStatus.ACTIVE), any(), any()))
            .thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIdIn(List.of(10L))).thenReturn(List.of(item));
        when(customerRepository.findAllById(List.of(1L))).thenReturn(List.of(customer));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(operator));

        ReportService.ReportFile reportFile = reportService.exportSales(from, to, "csv");
        String csv = new String(reportFile.content(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("lineTotal,operator"));
        assertTrue(csv.contains("\"Alice Admin (admin)\""));
    }

    @Test
    void exportLossesShouldGenerateCsvWithReadableOperator() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        Destruction destruction = buildDestruction();
        Batch batch = buildBatch();
        Product product = buildProduct();
        User operator = buildOperator();

        when(destructionRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(any(), any()))
            .thenReturn(List.of(destruction));
        when(batchRepository.findAllById(List.of(100L))).thenReturn(List.of(batch));
        when(productRepository.findAllById(List.of(5L))).thenReturn(List.of(product));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(operator));

        ReportService.ReportFile reportFile = reportService.exportLosses(from, to, "csv");
        String csv = new String(reportFile.content(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("lossAmount,operator,reason"));
        assertTrue(csv.contains("\"Alice Admin (admin)\",\"Expired stock\""));
    }

    @Test
    void exportSalesShouldRejectInvalidDateRange() {
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> reportService.exportSales(LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 1), "xlsx")
        );

        assertEquals("VALIDATION_ERROR", ex.getErrorCode().getCode());
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-20260426-001");
        order.setCustomerId(1L);
        order.setStatus(OrderStatus.ACTIVE);
        order.setSubtotalAmount(new BigDecimal("40.00"));
        order.setGstAmount(new BigDecimal("6.00"));
        order.setTotalAmount(new BigDecimal("46.00"));
        order.setCreatedAt(OffsetDateTime.parse("2026-04-26T10:15:30+12:00"));
        order.setUpdatedAt(OffsetDateTime.parse("2026-04-26T10:15:30+12:00"));
        order.setCreatedBy(7L);
        order.setUpdatedBy(7L);
        return order;
    }

    private OrderItem buildOrderItem() {
        OrderItem item = new OrderItem();
        item.setId(100L);
        item.setOrderId(10L);
        item.setProductId(5L);
        item.setBatchId(100L);
        item.setProductSkuSnapshot("DUMP001");
        item.setProductNameSnapshot("Frozen Dumplings");
        item.setProductUnitSnapshot("carton");
        item.setProductSpecSnapshot("1kg x 10");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setSubtotal(new BigDecimal("40.00"));
        item.setGstApplicable(true);
        item.setGstAmount(new BigDecimal("6.00"));
        item.setCreatedAt(OffsetDateTime.parse("2026-04-26T10:15:30+12:00"));
        return item;
    }

    private Customer buildCustomer() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("Panto Trading Ltd");
        customer.setActive(true);
        customer.setCreatedAt(OffsetDateTime.now().minusDays(1));
        customer.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        customer.setCreatedBy(1L);
        customer.setUpdatedBy(1L);
        return customer;
    }

    private Destruction buildDestruction() {
        Destruction destruction = new Destruction();
        destruction.setId(500L);
        destruction.setDestructionNumber("DES-20260426-001");
        destruction.setBatchId(100L);
        destruction.setProductId(5L);
        destruction.setInventoryTransactionId(900L);
        destruction.setQuantityDestroyed(4);
        destruction.setPurchaseUnitPriceSnapshot(new BigDecimal("12.50"));
        destruction.setLossAmount(new BigDecimal("50.00"));
        destruction.setReason("Expired stock");
        destruction.setCreatedAt(OffsetDateTime.parse("2026-04-26T11:30:00+12:00"));
        destruction.setCreatedBy(7L);
        return destruction;
    }

    private Batch buildBatch() {
        Batch batch = new Batch();
        batch.setId(100L);
        batch.setProductId(5L);
        batch.setInboundItemId(200L);
        batch.setBatchNumber("BATCH-001");
        batch.setArrivalDate(LocalDate.of(2026, 4, 10));
        batch.setExpiryDate(LocalDate.of(2026, 4, 28));
        batch.setQuantityReceived(20);
        batch.setQuantityRemaining(8);
        batch.setPurchaseUnitPrice(new BigDecimal("12.50"));
        batch.setExpiryStatus(ExpiryStatus.EXPIRED);
        batch.setVersion(0);
        batch.setCreatedAt(OffsetDateTime.now().minusDays(10));
        batch.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        batch.setCreatedBy(1L);
        batch.setUpdatedBy(1L);
        return batch;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(5L);
        product.setSku("DUMP001");
        product.setName("Frozen Dumplings");
        product.setCategory("Frozen");
        product.setSpecification("1kg x 10");
        product.setUnit("carton");
        product.setReferencePurchasePrice(new BigDecimal("12.50"));
        product.setReferenceSalePrice(new BigDecimal("20.00"));
        product.setSafetyStock(5);
        product.setGstApplicable(true);
        product.setActive(true);
        product.setCreatedAt(OffsetDateTime.now().minusDays(30));
        product.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        product.setCreatedBy(1L);
        product.setUpdatedBy(1L);
        return product;
    }

    private User buildOperator() {
        User user = new User();
        user.setId(7L);
        user.setUsername("admin");
        user.setFullName("Alice Admin");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setMustChangePassword(false);
        user.setPasswordHash("hash");
        user.setCreatedAt(OffsetDateTime.now().minusDays(10));
        user.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        user.setCreatedBy(1L);
        user.setUpdatedBy(1L);
        return user;
    }
}
