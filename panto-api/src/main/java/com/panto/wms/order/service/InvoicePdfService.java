package com.panto.wms.order.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.panto.wms.order.dto.InvoiceLineResponse;
import com.panto.wms.order.dto.InvoiceResponse;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * 发票 PDF 生成服务。
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    /**
     * 生成双联发票 PDF。
     *
     * @param invoice 发票数据
     * @return PDF 二进制内容
     */
    public byte[] generatePdf(InvoiceResponse invoice) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 48, 48);
            PdfWriter.getInstance(document, outputStream);
            document.open();
            renderCopy(document, invoice, "Customer Copy");
            document.newPage();
            renderCopy(document, invoice, "Office Copy");
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate invoice PDF", ex);
        }
    }

    private void renderCopy(Document document, InvoiceResponse invoice, String copyLabel) throws DocumentException {
        document.add(title(copyLabel));
        document.add(spacer(6));
        document.add(companyBlock(invoice));
        document.add(spacer(10));
        document.add(summaryTable(invoice));
        document.add(spacer(10));
        document.add(partiesTable(invoice));
        document.add(spacer(10));
        document.add(itemsTable(invoice));
        document.add(spacer(10));
        document.add(totalsTable(invoice));
        document.add(spacer(8));
        document.add(notesBlock(invoice));
    }

    private Paragraph title(String copyLabel) {
        Paragraph paragraph = new Paragraph("Tax Invoice - " + copyLabel, font(FontFactory.HELVETICA_BOLD, 18));
        paragraph.setAlignment(Element.ALIGN_LEFT);
        return paragraph;
    }

    private Paragraph companyBlock(InvoiceResponse invoice) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(invoice.seller().companyName() + "\n", font(FontFactory.HELVETICA_BOLD, 13)));
        paragraph.add(new Phrase(blankFallback(invoice.seller().address()) + "\n", font(FontFactory.HELVETICA, 10)));
        paragraph.add(new Phrase("Phone: " + blankFallback(invoice.seller().phone()) + "\n", font(FontFactory.HELVETICA, 10)));
        paragraph.add(new Phrase("Email: " + blankFallback(invoice.seller().email()) + "\n", font(FontFactory.HELVETICA, 10)));
        paragraph.add(new Phrase("GST Number: " + blankFallback(invoice.seller().gstNumber()), font(FontFactory.HELVETICA, 10)));
        return paragraph;
    }

    private PdfPTable summaryTable(InvoiceResponse invoice) {
        PdfPTable table = new PdfPTable(new float[] { 1f, 1f, 1f });
        table.setWidthPercentage(100);
        table.addCell(labelCell("Invoice Number"));
        table.addCell(labelCell("Invoice Date"));
        table.addCell(labelCell("Status"));
        table.addCell(valueCell(invoice.invoiceNumber()));
        table.addCell(valueCell(formatDateTime(invoice.invoiceDate())));
        table.addCell(valueCell(invoice.status().name()));
        return table;
    }

    private PdfPTable partiesTable(InvoiceResponse invoice) {
        PdfPTable table = new PdfPTable(new float[] { 1f, 1f });
        table.setWidthPercentage(100);
        table.addCell(multilineCell(
            "Seller",
            invoice.seller().companyName(),
            blankFallback(invoice.seller().address()),
            "Phone: " + blankFallback(invoice.seller().phone()),
            "Email: " + blankFallback(invoice.seller().email()),
            "GST: " + blankFallback(invoice.seller().gstNumber())
        ));
        table.addCell(multilineCell(
            "Buyer",
            invoice.customer().companyName(),
            blankFallback(invoice.customer().address()),
            "Contact: " + blankFallback(invoice.customer().contactPerson()),
            "Phone: " + blankFallback(invoice.customer().phone()),
            "GST: " + blankFallback(invoice.customer().gstNumber())
        ));
        return table;
    }

    private PdfPTable itemsTable(InvoiceResponse invoice) {
        PdfPTable table = new PdfPTable(new float[] { 2.3f, 1f, 0.8f, 1f, 1f, 1f });
        table.setWidthPercentage(100);
        table.addCell(labelCell("Item"));
        table.addCell(labelCell("Specification"));
        table.addCell(labelCell("Unit"));
        table.addCell(labelCell("Qty"));
        table.addCell(labelCell("Unit Price"));
        table.addCell(labelCell("Subtotal"));

        for (InvoiceLineResponse item : invoice.items()) {
            table.addCell(valueCell(item.productName() + " (" + item.productSku() + ")"));
            table.addCell(valueCell(blankFallback(item.productSpecification())));
            table.addCell(valueCell(item.productUnit()));
            table.addCell(valueCell(String.valueOf(item.quantity())));
            table.addCell(valueCell(formatAmount(item.unitPrice())));
            table.addCell(valueCell(formatAmount(item.subtotal())));
        }

        return table;
    }

    private PdfPTable totalsTable(InvoiceResponse invoice) {
        PdfPTable table = new PdfPTable(new float[] { 2f, 1f });
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setTotalWidth(220);
        table.setLockedWidth(true);
        table.addCell(labelCell("Subtotal (excl. GST)"));
        table.addCell(valueCell(formatAmount(invoice.subtotalAmount())));
        table.addCell(labelCell("GST"));
        table.addCell(valueCell(formatAmount(invoice.gstAmount())));
        table.addCell(labelCell("Total (incl. GST)"));
        table.addCell(valueCell(formatAmount(invoice.totalAmount())));
        return table;
    }

    private Paragraph notesBlock(InvoiceResponse invoice) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase("Payment Instructions: ", font(FontFactory.HELVETICA_BOLD, 10)));
        paragraph.add(new Phrase(blankFallback(invoice.paymentInstructions()) + "\n", font(FontFactory.HELVETICA, 10)));
        paragraph.add(new Phrase("Remarks: ", font(FontFactory.HELVETICA_BOLD, 10)));
        paragraph.add(new Phrase(blankFallback(invoice.remarks()), font(FontFactory.HELVETICA, 10)));
        return paragraph;
    }

    private PdfPCell labelCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font(FontFactory.HELVETICA_BOLD, 10)));
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell valueCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font(FontFactory.HELVETICA, 10)));
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell multilineCell(String title, String... lines) {
        Paragraph paragraph = new Paragraph(title + "\n", font(FontFactory.HELVETICA_BOLD, 10));
        for (String line : lines) {
            paragraph.add(new Phrase(line + "\n", font(FontFactory.HELVETICA, 10)));
        }

        PdfPCell cell = new PdfPCell(paragraph);
        cell.setPadding(8);
        cell.setMinimumHeight(110);
        return cell;
    }

    private Paragraph spacer(float height) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setLeading(height);
        return paragraph;
    }

    private Font font(String family, float size) {
        return FontFactory.getFont(family, size);
    }

    private String formatDateTime(OffsetDateTime value) {
        return value.format(DATE_TIME_FORMAT);
    }

    private String formatAmount(BigDecimal value) {
        return "$" + value.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "Not configured" : value;
    }
}
