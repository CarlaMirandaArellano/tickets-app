package cl.duoc.tickets.service;

import cl.duoc.tickets.model.Ticket;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Genera el PDF personalizado de cada ticket. */
@Component
public class PdfTicketGenerator {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] generar(Ticket t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(0x1F, 0x3A, 0x68));
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
            Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valor = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph p = new Paragraph(texto(t.getEventoNombre()), titulo);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);

            Paragraph sub = new Paragraph("TICKET " + t.getCodigo().toUpperCase() + "  ·  " + t.getEstado(), subtitulo);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(14);
            doc.add(sub);

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1.2f, 3f});

            fila(tabla, "Evento", t.getEventoId() + " - " + texto(t.getEventoNombre()), etiqueta, valor);
            fila(tabla, "Fecha", fecha(t.getFechaEvento()), etiqueta, valor);
            fila(tabla, "Lugar", texto(t.getLugar()), etiqueta, valor);
            fila(tabla, "Asistente", texto(t.getUsuarioNombre()) + " (" + t.getUsuarioId() + ")", etiqueta, valor);
            fila(tabla, "Email", texto(t.getUsuarioEmail()), etiqueta, valor);
            fila(tabla, "Entrada", texto(t.getTipoEntrada()), etiqueta, valor);
            fila(tabla, "Asiento", texto(t.getAsiento()), etiqueta, valor);
            fila(tabla, "Precio", t.getPrecio() == null ? "-" : "$" + t.getPrecio().toPlainString(), etiqueta, valor);
            doc.add(tabla);

            Paragraph pie = new Paragraph("Emitido: " + fecha(LocalDateTime.now())
                    + "   ·   Presenta este ticket en la entrada del evento.", valor);
            pie.setSpacingBefore(14);
            doc.add(pie);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del ticket", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    private void fila(PdfPTable tabla, String nombre, String contenido, Font fEtiqueta, Font fValor) {
        PdfPCell c1 = new PdfPCell(new Phrase(nombre, fEtiqueta));
        c1.setBackgroundColor(new Color(0xE8, 0xEE, 0xF7));
        c1.setPadding(5);
        PdfPCell c2 = new PdfPCell(new Phrase(contenido, fValor));
        c2.setPadding(5);
        tabla.addCell(c1);
        tabla.addCell(c2);
    }

    private String texto(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private String fecha(LocalDateTime f) {
        return f == null ? "-" : f.format(FORMATO);
    }
}
