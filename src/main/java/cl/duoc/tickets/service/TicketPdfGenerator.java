package cl.duoc.tickets.service;

import cl.duoc.tickets.model.Ticket;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Genera el PDF personalizado de un ticket. */
@Component
public class TicketPdfGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] generar(Ticket t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(25, 60, 130));
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
        Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valor = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font pie = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

        doc.add(new Paragraph("TICKET - " + t.getEventoNombre(), titulo));
        doc.add(new Paragraph("Código: " + t.getNombreArchivo().replace(".pdf", "").toUpperCase()
                + "   |   Estado: " + t.getEstado(), subtitulo));
        doc.add(Chunk.NEWLINE);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1, 2.5f});
        fila(tabla, "Evento", t.getEventoNombre() + " (" + t.getEventoId() + ")", etiqueta, valor);
        fila(tabla, "Fecha del evento", t.getFechaEvento() == null ? "-" : t.getFechaEvento().format(FECHA), etiqueta, valor);
        fila(tabla, "Lugar", nvl(t.getLugar()), etiqueta, valor);
        fila(tabla, "Asistente", t.getUsuarioNombre() + " (" + t.getUsuarioId() + ")", etiqueta, valor);
        fila(tabla, "Email", nvl(t.getUsuarioEmail()), etiqueta, valor);
        fila(tabla, "Asiento", nvl(t.getAsiento()), etiqueta, valor);
        fila(tabla, "Precio", NumberFormat.getCurrencyInstance(Locale.of("es", "CL")).format(t.getPrecio()), etiqueta, valor);
        doc.add(tabla);

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Versión del ticket: " + t.getVersion()
                + "  ·  Generado: " + LocalDateTime.now().format(FECHA), pie));
        doc.close();
        return out.toByteArray();
    }

    private static void fila(PdfPTable tabla, String k, String v, Font fk, Font fv) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, fk));
        c1.setBackgroundColor(new Color(230, 236, 248));
        c1.setPadding(5);
        PdfPCell c2 = new PdfPCell(new Phrase(v, fv));
        c2.setPadding(5);
        tabla.addCell(c1);
        tabla.addCell(c2);
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
