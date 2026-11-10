package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.exception.NotFoundException;
import cl.duoc.tickets.model.AccionArchivo;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.HistorialArchivo;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.repository.HistorialArchivoRepository;
import cl.duoc.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository tickets;
    private final HistorialArchivoRepository historial;
    private final PdfTicketGenerator pdf;
    private final EfsStorageService efs;
    private final S3StorageService s3;
    private final boolean autoUpload;

    public TicketService(TicketRepository tickets,
                         HistorialArchivoRepository historial,
                         PdfTicketGenerator pdf,
                         EfsStorageService efs,
                         S3StorageService s3,
                         @Value("${app.s3.auto-upload}") boolean autoUpload) {
        this.tickets = tickets;
        this.historial = historial;
        this.pdf = pdf;
        this.efs = efs;
        this.s3 = s3;
        this.autoUpload = autoUpload;
    }

    // ------------------------------------------------------------------ Generar

    /** Genera el ticket: lo registra, crea el PDF en el EFS y (si está activo) lo sube a S3. */
    @Transactional
    public Ticket generar(TicketRequest req) {
        Ticket t = new Ticket();
        t.setEventoId(req.eventoId());
        t.setEventoNombre(req.eventoNombre());
        t.setFechaEvento(req.fechaEvento());
        t.setLugar(req.lugar());
        t.setUsuarioId(req.usuarioId());
        t.setUsuarioNombre(req.usuarioNombre());
        t.setUsuarioEmail(req.usuarioEmail());
        t.setTipoEntrada(req.tipoEntrada() == null ? "General" : req.tipoEntrada());
        t.setAsiento(req.asiento());
        t.setPrecio(req.precio());
        t.setEstado(req.estado() == null ? EstadoTicket.RESERVADO : req.estado());

        t = tickets.save(t);                                  // obtiene el id
        t.setCodigo(String.format("ticket%03d", t.getId()));  // ticket001, ticket002, ...

        escribirEnEfs(t, AccionArchivo.GENERADO_EFS, "Ticket generado");

        if (autoUpload && s3.configurado()) {
            subirArchivo(t);
        }
        return tickets.save(t);
    }

    // ------------------------------------------------------------------ Subir a S3

    @Transactional
    public Ticket subirAS3(Long id) {
        Ticket t = obtener(id);
        subirArchivo(t);
        return tickets.save(t);
    }

    /** Sube a S3 todos los tickets que aún están solo en el EFS. */
    @Transactional
    public List<Ticket> subirPendientes() {
        List<Ticket> pendientes = tickets.findBySubidoS3False();
        for (Ticket t : pendientes) {
            subirArchivo(t);
        }
        return tickets.saveAll(pendientes);
    }

    // ------------------------------------------------------------------ Consultar

    public Ticket obtener(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ticket con id " + id));
    }

    /** Historial de archivos generados, con filtros opcionales. */
    public List<Ticket> listar(String eventoId, String usuarioId, EstadoTicket estado,
                               Boolean subidoS3, LocalDate desde, LocalDate hasta) {
        List<Ticket> base;
        if (eventoId != null && usuarioId != null) {
            base = tickets.findByEventoIdAndUsuarioId(eventoId, usuarioId);
        } else if (eventoId != null) {
            base = tickets.findByEventoId(eventoId);
        } else if (usuarioId != null) {
            base = tickets.findByUsuarioId(usuarioId);
        } else {
            base = tickets.findAll();
        }
        return base.stream()
                .filter(t -> estado == null || t.getEstado() == estado)
                .filter(t -> subidoS3 == null || t.isSubidoS3() == subidoS3)
                .filter(t -> desde == null || !t.getFechaCreacion().toLocalDate().isBefore(desde))
                .filter(t -> hasta == null || !t.getFechaCreacion().toLocalDate().isAfter(hasta))
                .sorted(Comparator.comparing(Ticket::getId))
                .toList();
    }

    public List<HistorialArchivo> historial(Long ticketId, String eventoId, String usuarioId,
                                            AccionArchivo accion, LocalDate desde, LocalDate hasta) {
        return historial.findAll().stream()
                .filter(h -> ticketId == null || ticketId.equals(h.getTicketId()))
                .filter(h -> eventoId == null || eventoId.equals(h.getEventoId()))
                .filter(h -> usuarioId == null || usuarioId.equals(h.getUsuarioId()))
                .filter(h -> accion == null || h.getAccion() == accion)
                .filter(h -> desde == null || !h.getFecha().toLocalDate().isBefore(desde))
                .filter(h -> hasta == null || !h.getFecha().toLocalDate().isAfter(hasta))
                .sorted(Comparator.comparing(HistorialArchivo::getFecha).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ Descargar

    /** Descarga el PDF de un ticket desde S3. */
    @Transactional
    public byte[] descargar(Long id) {
        Ticket t = obtener(id);
        if (!t.isSubidoS3()) {
            throw new IllegalStateException("El ticket " + t.getCodigo()
                    + " todavía no está en S3. Súbelo con POST /api/tickets/" + id + "/subir");
        }
        byte[] contenido = s3.descargar(t.getS3Key());
        registrar(t, AccionArchivo.DESCARGADO, "s3://" + s3.getBucket() + "/" + t.getS3Key(), "Descarga individual");
        return contenido;
    }

    /** Descarga en un ZIP todos los tickets (desde S3) de un evento y/o de un usuario. */
    @Transactional
    public byte[] descargarZip(String eventoId, String usuarioId) {
        if (eventoId == null && usuarioId == null) {
            throw new IllegalArgumentException("Indica eventoId y/o usuarioId");
        }
        List<Ticket> lista = listar(eventoId, usuarioId, null, true, null, null);
        if (lista.isEmpty()) {
            throw new NotFoundException("No hay tickets en S3 para los filtros indicados");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Ticket t : lista) {
                zip.putNextEntry(new ZipEntry(t.getS3Key()));   // mantiene evento/usuario/ticket.pdf
                zip.write(s3.descargar(t.getS3Key()));
                zip.closeEntry();
                registrar(t, AccionArchivo.DESCARGADO, "s3://" + s3.getBucket() + "/" + t.getS3Key(), "Descarga en ZIP");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el ZIP", e);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ Modificar

    /**
     * Modifica los datos del ticket, regenera el PDF en el EFS y lo reemplaza en S3.
     * Si cambia el evento o el usuario, el archivo se mueve a la nueva carpeta.
     */
    @Transactional
    public Ticket actualizar(Long id, TicketUpdateRequest req) {
        Ticket t = obtener(id);
        String rutaEfsAnterior = t.getRutaEfs();
        String keyAnterior = t.getS3Key();
        boolean estabaEnS3 = t.isSubidoS3();

        if (req.eventoId() != null) t.setEventoId(req.eventoId());
        if (req.eventoNombre() != null) t.setEventoNombre(req.eventoNombre());
        if (req.fechaEvento() != null) t.setFechaEvento(req.fechaEvento());
        if (req.lugar() != null) t.setLugar(req.lugar());
        if (req.usuarioId() != null) t.setUsuarioId(req.usuarioId());
        if (req.usuarioNombre() != null) t.setUsuarioNombre(req.usuarioNombre());
        if (req.usuarioEmail() != null) t.setUsuarioEmail(req.usuarioEmail());
        if (req.tipoEntrada() != null) t.setTipoEntrada(req.tipoEntrada());
        if (req.asiento() != null) t.setAsiento(req.asiento());
        if (req.precio() != null) t.setPrecio(req.precio());
        if (req.estado() != null) t.setEstado(req.estado());

        escribirEnEfs(t, AccionArchivo.MODIFICADO, "PDF regenerado con los nuevos datos");

        // Si la ruta cambió, se borra el PDF antiguo del EFS
        if (rutaEfsAnterior != null && !rutaEfsAnterior.equals(t.getRutaEfs())) {
            efs.eliminar(rutaEfsAnterior);
        }

        if (s3.configurado() && (estabaEnS3 || autoUpload)) {
            subirArchivo(t);   // sobrescribe el objeto en S3
            String keyNueva = t.getS3Key();
            if (estabaEnS3 && keyAnterior != null && !keyAnterior.equals(keyNueva)) {
                s3.eliminar(keyAnterior);
                registrar(t, AccionArchivo.ELIMINADO, "s3://" + s3.getBucket() + "/" + keyAnterior,
                        "Movido a " + keyNueva);
            }
        }
        return tickets.save(t);
    }

    // ------------------------------------------------------------------ Eliminar

    /** Elimina el ticket de S3, del EFS y de la base de datos (el historial se conserva). */
    @Transactional
    public void eliminar(Long id) {
        Ticket t = obtener(id);
        if (t.isSubidoS3() && s3.configurado()) {
            s3.eliminar(t.getS3Key());
        }
        efs.eliminar(t.getRutaEfs());
        registrar(t, AccionArchivo.ELIMINADO,
                t.isSubidoS3() ? "s3://" + s3.getBucket() + "/" + t.getS3Key() : t.getRutaEfs(),
                "Ticket eliminado de S3, EFS y base de datos");
        tickets.delete(t);
    }

    // ------------------------------------------------------------------ Internos

    private void escribirEnEfs(Ticket t, AccionArchivo accion, String detalle) {
        byte[] contenido = pdf.generar(t);
        Path ruta = efs.guardar(t.getEventoId(), t.getUsuarioId(), t.getCodigo(), contenido);
        t.setRutaEfs(ruta.toString());
        registrar(t, accion, ruta.toString(), detalle);
    }

    /** Lee el PDF desde el EFS y lo sube a S3 en evento/usuario/ticketXXX.pdf */
    private void subirArchivo(Ticket t) {
        if (!efs.existe(t.getRutaEfs())) {
            // Si el archivo temporal ya no está, se regenera
            escribirEnEfs(t, AccionArchivo.GENERADO_EFS, "Regenerado antes de subir a S3");
        }
        String key = S3StorageService.construirKey(t.getEventoId(), t.getUsuarioId(), t.getCodigo());
        s3.subir(key, efs.leer(t.getRutaEfs()), Map.of(
                "evento-id", t.getEventoId(),
                "usuario-id", t.getUsuarioId(),
                "codigo", t.getCodigo(),
                "estado", t.getEstado().name()));
        t.setS3Key(key);
        t.setSubidoS3(true);
        t.setFechaSubidaS3(LocalDateTime.now());
        registrar(t, AccionArchivo.SUBIDO_S3, "s3://" + s3.getBucket() + "/" + key, "Subido a S3");
        log.info("Ticket {} disponible en S3", t.getCodigo());
    }

    private void registrar(Ticket t, AccionArchivo accion, String ubicacion, String detalle) {
        historial.save(new HistorialArchivo(t, accion, ubicacion, detalle));
    }
}
