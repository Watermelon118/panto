package com.panto.wms.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.dto.InvoiceCustomerResponse;
import com.panto.wms.order.dto.InvoiceLineResponse;
import com.panto.wms.order.dto.InvoiceResponse;
import com.panto.wms.order.dto.InvoiceSellerResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 发票 PDF 生成服务测试。
 */
class InvoicePdfServiceTest {

    private final InvoicePdfService invoicePdfService = new InvoicePdfService();

    @Test
    void generatePdfShouldReturnPdfBinary() {
        InvoiceResponse invoice = new InvoiceResponse(
            500L,
            "ORD-20260425-001",
            OffsetDateTime.parse("2026-04-25T10:00:00+12:00"),
            OrderStatus.ACTIVE,
            new InvoiceSellerResponse(
                "Panto Trading Ltd",
                "GST-9988",
                "1 Queen Street, Auckland",
                "09 123 4567",
                "accounts@panto.co.nz"
            ),
            new InvoiceCustomerResponse(
                "Fresh Dumplings Ltd",
                "Alex Chen",
                "021888999",
                "99 Queen Street",
                "GST-7788"
            ),
            List.of(
                new InvoiceLineResponse(
                    "DUMP001",
                    "Frozen Dumplings",
                    "1kg x 10",
                    "carton",
                    12,
                    new BigDecimal("20.00"),
                    new BigDecimal("240.00"),
                    true,
                    new BigDecimal("36.00")
                )
            ),
            new BigDecimal("240.00"),
            new BigDecimal("36.00"),
            new BigDecimal("276.00"),
            "Deliver before noon",
            "Bank transfer"
        );

        byte[] pdf = invoicePdfService.generatePdf(invoice);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
