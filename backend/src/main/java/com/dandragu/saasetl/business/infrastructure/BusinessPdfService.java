package com.dandragu.saasetl.business.infrastructure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.dandragu.saasetl.business.application.BusinessModels.DocumentLine;
import com.dandragu.saasetl.business.application.BusinessModels.Invoice;
import com.dandragu.saasetl.business.application.BusinessModels.Quote;

@Service
public class BusinessPdfService {
	private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

	public byte[] quote(Quote quote) {
		return document("PRESUPUESTO", quote.number(), quote.customerName(), quote.issueDate(),
				quote.validUntil(), "Válido hasta", quote.lines(), quote.subtotal(), quote.taxTotal(),
				quote.total(), quote.notes());
	}

	public byte[] invoice(Invoice invoice) {
		return document("FACTURA", invoice.number(), invoice.customerName(), invoice.issueDate(),
				invoice.dueDate(), "Vencimiento", invoice.lines(), invoice.subtotal(), invoice.taxTotal(),
				invoice.total(), invoice.notes());
	}

	private byte[] document(String type, String number, String customer, LocalDate issueDate,
			LocalDate secondaryDate, String secondaryLabel, List<DocumentLine> lines,
			BigDecimal subtotal, BigDecimal tax, BigDecimal total, String notes) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.setNonStrokingColor(18, 55, 86);
				content.addRect(0, 760, 595, 82);
				content.fill();
				text(content, BOLD, 22, 42, 800, "NEXO · SERVICIOS ELÉCTRICOS");
				text(content, REGULAR, 10, 42, 781, "Instalaciones, mantenimiento y reformas");
				content.setNonStrokingColor(31, 41, 55);
				text(content, BOLD, 20, 390, 720, type);
				text(content, BOLD, 12, 390, 699, number);
				text(content, BOLD, 11, 42, 720, "Cliente");
				text(content, REGULAR, 12, 42, 699, customer);
				text(content, REGULAR, 10, 42, 678, "Fecha: " + issueDate);
				text(content, REGULAR, 10, 250, 678, secondaryLabel + ": " + secondaryDate);

				float y = 635;
				content.setNonStrokingColor(232, 240, 247);
				content.addRect(42, y, 511, 24);
				content.fill();
				content.setNonStrokingColor(31, 41, 55);
				text(content, BOLD, 9, 50, y + 8, "CONCEPTO");
				text(content, BOLD, 9, 335, y + 8, "CANT.");
				text(content, BOLD, 9, 395, y + 8, "PRECIO");
				text(content, BOLD, 9, 470, y + 8, "TOTAL");
				y -= 20;
				for (DocumentLine line : lines) {
					text(content, REGULAR, 9, 50, y, shorten(line.description(), 48));
					text(content, REGULAR, 9, 335, y, line.quantity().stripTrailingZeros().toPlainString());
					text(content, REGULAR, 9, 395, y, money(line.unitPrice()));
					text(content, REGULAR, 9, 470, y, money(line.total()));
					y -= 22;
				}
				float totalsY = Math.max(180, y - 25);
				text(content, REGULAR, 10, 390, totalsY, "Base imponible");
				text(content, REGULAR, 10, 490, totalsY, money(subtotal));
				text(content, REGULAR, 10, 390, totalsY - 20, "IVA");
				text(content, REGULAR, 10, 490, totalsY - 20, money(tax));
				text(content, BOLD, 13, 390, totalsY - 47, "TOTAL");
				text(content, BOLD, 13, 485, totalsY - 47, money(total));
				if (notes != null && !notes.isBlank()) {
					text(content, BOLD, 9, 42, 145, "Observaciones");
					text(content, REGULAR, 8, 42, 130, shorten(notes, 105));
				}
				content.setNonStrokingColor(100, 116, 139);
				text(content, REGULAR, 8, 42, 42,
						"Documento de demostración · Prototipo no certificado para facturación fiscal.");
			}
			document.save(output);
			return output.toByteArray();
		}
		catch (IOException exception) {
			throw new IllegalStateException("Could not generate PDF", exception);
		}
	}

	private void text(PDPageContentStream content, PDType1Font font, float size,
			float x, float y, String value) throws IOException {
		content.beginText();
		content.setFont(font, size);
		content.newLineAtOffset(x, y);
		content.showText(safe(value));
		content.endText();
	}

	private String safe(String value) {
		return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ')
				.replace('€', 'E');
	}

	private String shorten(String value, int length) {
		return value.length() <= length ? value : value.substring(0, length - 1) + "…";
	}

	private String money(BigDecimal value) {
		return value.setScale(2) + " EUR";
	}
}
