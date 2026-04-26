package com.panto.wms.reports.service;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.customer.entity.Customer;
import com.panto.wms.customer.repository.CustomerRepository;
import com.panto.wms.destruction.entity.Destruction;
import com.panto.wms.destruction.repository.DestructionRepository;
import com.panto.wms.inventory.entity.Batch;
import com.panto.wms.inventory.repository.BatchRepository;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.entity.OrderItem;
import com.panto.wms.order.repository.OrderItemRepository;
import com.panto.wms.order.repository.OrderRepository;
import com.panto.wms.product.entity.Product;
import com.panto.wms.product.repository.ProductRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 财务报表导出业务服务。
 */
@Service
public class ReportService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String FORMAT_CSV = "csv";
    private static final String FORMAT_XLSX = "xlsx";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final DestructionRepository destructionRepository;
    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;

    /**
     * 创建报表导出业务服务。
     *
     * @param orderRepository 订单仓储
     * @param orderItemRepository 订单明细仓储
     * @param customerRepository 客户仓储
     * @param destructionRepository 销毁记录仓储
     * @param batchRepository 批次仓储
     * @param productRepository 商品仓储
     */
    public ReportService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        CustomerRepository customerRepository,
        DestructionRepository destructionRepository,
        BatchRepository batchRepository,
        ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
        this.destructionRepository = destructionRepository;
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
    }

    /**
     * 导出销售报表。
     *
     * @param from 起始日期（含）
     * @param to 结束日期（含）
     * @param format 导出格式，支持 csv / xlsx
     * @return 导出文件
     */
    @Transactional(readOnly = true)
    public ReportFile exportSales(LocalDate from, LocalDate to, String format) {
        validateDateRange(from, to);
        String normalizedFormat = normalizeFormat(format);

        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toStartOfNextDay(to);
        List<Order> orders = orderRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus.ACTIVE,
            start,
            end
        ).stream()
            .sorted(Comparator.comparing(Order::getCreatedAt).thenComparing(Order::getId))
            .toList();

        List<SalesRow> rows = buildSalesRows(orders);
        String filePrefix = "sales-report-%s-to-%s".formatted(
            from.format(FILE_DATE_FORMAT),
            to.format(FILE_DATE_FORMAT)
        );

        return switch (normalizedFormat) {
            case FORMAT_CSV -> new ReportFile(
                filePrefix + ".csv",
                "text/csv; charset=UTF-8",
                buildSalesCsv(rows)
            );
            case FORMAT_XLSX -> new ReportFile(
                filePrefix + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildSalesWorkbook(rows)
            );
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "format must be csv or xlsx");
        };
    }

    /**
     * 导出损耗报表。
     *
     * @param from 起始日期（含）
     * @param to 结束日期（含）
     * @param format 导出格式，支持 csv / xlsx
     * @return 导出文件
     */
    @Transactional(readOnly = true)
    public ReportFile exportLosses(LocalDate from, LocalDate to, String format) {
        validateDateRange(from, to);
        String normalizedFormat = normalizeFormat(format);

        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toStartOfNextDay(to);
        List<Destruction> destructions =
            destructionRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
                start,
                end
            );

        List<LossRow> rows = buildLossRows(destructions);
        String filePrefix = "loss-report-%s-to-%s".formatted(
            from.format(FILE_DATE_FORMAT),
            to.format(FILE_DATE_FORMAT)
        );

        return switch (normalizedFormat) {
            case FORMAT_CSV -> new ReportFile(
                filePrefix + ".csv",
                "text/csv; charset=UTF-8",
                buildLossCsv(rows)
            );
            case FORMAT_XLSX -> new ReportFile(
                filePrefix + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildLossWorkbook(rows)
            );
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "format must be csv or xlsx");
        };
    }

    private List<SalesRow> buildSalesRows(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, Customer> customerMap = loadCustomerMap(
            orders.stream().map(Order::getCustomerId).distinct().toList()
        );
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(
            orders.stream().map(Order::getId).toList()
        ).stream()
            .sorted(Comparator.comparing(OrderItem::getId))
            .collect(Collectors.groupingBy(OrderItem::getOrderId, LinkedHashMap::new, Collectors.toList()));

        return orders.stream()
            .flatMap(order -> {
                Customer customer = customerMap.get(order.getCustomerId());
                return itemsByOrderId.getOrDefault(order.getId(), List.of()).stream()
                    .map(item -> toSalesRow(order, customer, item));
            })
            .toList();
    }

    private List<LossRow> buildLossRows(List<Destruction> destructions) {
        if (destructions.isEmpty()) {
            return List.of();
        }

        Map<Long, Batch> batchMap = loadBatchMap(
            destructions.stream().map(Destruction::getBatchId).distinct().toList()
        );
        Map<Long, Product> productMap = loadProductMap(
            destructions.stream().map(Destruction::getProductId).distinct().toList()
        );

        return destructions.stream()
            .map(destruction -> toLossRow(
                destruction,
                batchMap.get(destruction.getBatchId()),
                productMap.get(destruction.getProductId())
            ))
            .toList();
    }

    private SalesRow toSalesRow(Order order, Customer customer, OrderItem item) {
        BigDecimal lineTotal = item.getSubtotal().add(item.getGstAmount()).setScale(2, RoundingMode.HALF_UP);
        return new SalesRow(
            order.getOrderNumber(),
            order.getCreatedAt(),
            customer != null ? customer.getCompanyName() : null,
            item.getProductSkuSnapshot(),
            item.getProductNameSnapshot(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getSubtotal(),
            item.getGstAmount(),
            lineTotal,
            order.getCreatedBy()
        );
    }

    private byte[] buildSalesCsv(List<SalesRow> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(
            "orderNumber,orderCreatedAt,customerCompanyName,productSku,productName,quantity,unitPrice,subtotal,gstAmount,lineTotal,operatorId\n"
        );
        for (SalesRow row : rows) {
            builder.append(csv(row.orderNumber())).append(',')
                .append(csv(row.orderCreatedAt().toString())).append(',')
                .append(csv(row.customerCompanyName())).append(',')
                .append(csv(row.productSku())).append(',')
                .append(csv(row.productName())).append(',')
                .append(row.quantity()).append(',')
                .append(row.unitPrice()).append(',')
                .append(row.subtotal()).append(',')
                .append(row.gstAmount()).append(',')
                .append(row.lineTotal()).append(',')
                .append(row.operatorId())
                .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildSalesWorkbook(List<SalesRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Sales");
            String[] headers = {
                "Order Number",
                "Order Created At",
                "Customer",
                "Product SKU",
                "Product Name",
                "Quantity",
                "Unit Price",
                "Subtotal",
                "GST Amount",
                "Line Total",
                "Operator ID"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < rows.size(); i++) {
                SalesRow row = rows.get(i);
                Row excelRow = sheet.createRow(i + 1);
                writeCell(excelRow, 0, row.orderNumber());
                writeCell(excelRow, 1, row.orderCreatedAt().toString());
                writeCell(excelRow, 2, row.customerCompanyName());
                writeCell(excelRow, 3, row.productSku());
                writeCell(excelRow, 4, row.productName());
                excelRow.createCell(5).setCellValue(row.quantity());
                excelRow.createCell(6).setCellValue(row.unitPrice().doubleValue());
                excelRow.createCell(7).setCellValue(row.subtotal().doubleValue());
                excelRow.createCell(8).setCellValue(row.gstAmount().doubleValue());
                excelRow.createCell(9).setCellValue(row.lineTotal().doubleValue());
                excelRow.createCell(10).setCellValue(row.operatorId());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build sales workbook", ex);
        }
    }

    private byte[] buildLossCsv(List<LossRow> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(
            "destructionNumber,destructionCreatedAt,productSku,productName,batchNumber,quantityDestroyed,purchaseUnitPrice,lossAmount,operatorId,reason\n"
        );
        for (LossRow row : rows) {
            builder.append(csv(row.destructionNumber())).append(',')
                .append(csv(row.destructionCreatedAt().toString())).append(',')
                .append(csv(row.productSku())).append(',')
                .append(csv(row.productName())).append(',')
                .append(csv(row.batchNumber())).append(',')
                .append(row.quantityDestroyed()).append(',')
                .append(row.purchaseUnitPrice()).append(',')
                .append(row.lossAmount()).append(',')
                .append(row.operatorId()).append(',')
                .append(csv(row.reason()))
                .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildLossWorkbook(List<LossRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Losses");
            String[] headers = {
                "Destruction Number",
                "Destruction Created At",
                "Product SKU",
                "Product Name",
                "Batch Number",
                "Quantity Destroyed",
                "Purchase Unit Price",
                "Loss Amount",
                "Operator ID",
                "Reason"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < rows.size(); i++) {
                LossRow row = rows.get(i);
                Row excelRow = sheet.createRow(i + 1);
                writeCell(excelRow, 0, row.destructionNumber());
                writeCell(excelRow, 1, row.destructionCreatedAt().toString());
                writeCell(excelRow, 2, row.productSku());
                writeCell(excelRow, 3, row.productName());
                writeCell(excelRow, 4, row.batchNumber());
                excelRow.createCell(5).setCellValue(row.quantityDestroyed());
                excelRow.createCell(6).setCellValue(row.purchaseUnitPrice().doubleValue());
                excelRow.createCell(7).setCellValue(row.lossAmount().doubleValue());
                excelRow.createCell(8).setCellValue(row.operatorId());
                writeCell(excelRow, 9, row.reason());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build loss workbook", ex);
        }
    }

    private LossRow toLossRow(Destruction destruction, Batch batch, Product product) {
        return new LossRow(
            destruction.getDestructionNumber(),
            destruction.getCreatedAt(),
            product != null ? product.getSku() : null,
            product != null ? product.getName() : null,
            batch != null ? batch.getBatchNumber() : null,
            destruction.getQuantityDestroyed(),
            destruction.getPurchaseUnitPriceSnapshot(),
            destruction.getLossAmount(),
            destruction.getCreatedBy(),
            destruction.getReason()
        );
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

    private Map<Long, Product> loadProductMap(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "from and to are required");
        }
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "from must be on or before to");
        }
    }

    private String normalizeFormat(String format) {
        return Optional.ofNullable(format)
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(value -> !value.isEmpty())
            .orElse(FORMAT_XLSX);
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(currentOffset());
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay().atOffset(currentOffset());
    }

    private ZoneOffset currentOffset() {
        return OffsetDateTime.now().getOffset();
    }

    private String csv(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private void writeCell(Row row, int index, String value) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
    }

    /**
     * 导出文件载体。
     *
     * @param fileName 文件名
     * @param contentType 内容类型
     * @param content 文件二进制内容
     */
    public record ReportFile(String fileName, String contentType, byte[] content) {
    }

    private record SalesRow(
        String orderNumber,
        OffsetDateTime orderCreatedAt,
        String customerCompanyName,
        String productSku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal gstAmount,
        BigDecimal lineTotal,
        Long operatorId
    ) {
    }

    private record LossRow(
        String destructionNumber,
        OffsetDateTime destructionCreatedAt,
        String productSku,
        String productName,
        String batchNumber,
        Integer quantityDestroyed,
        BigDecimal purchaseUnitPrice,
        BigDecimal lossAmount,
        Long operatorId,
        String reason
    ) {
    }
}
