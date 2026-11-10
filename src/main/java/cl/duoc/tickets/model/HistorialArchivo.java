package cl.duoc.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de todo lo que ocurre con los archivos de tickets
 * (generación en EFS, subida a S3, modificación, descarga y eliminación).
 */
@Entity
@Table(name = "historial_archivos")
public class HistorialArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;
    private String codigo;
    private String eventoId;
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionArchivo accion;

    /** Ruta EFS o key S3 afectada */
    @Column(length = 500)
    private String ubicacion;

    @Column(length = 500)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected HistorialArchivo() {
    }

    public HistorialArchivo(Ticket ticket, AccionArchivo accion, String ubicacion, String detalle) {
        this.ticketId = ticket.getId();
        this.codigo = ticket.getCodigo();
        this.eventoId = ticket.getEventoId();
        this.usuarioId = ticket.getUsuarioId();
        this.accion = accion;
        this.ubicacion = ubicacion;
        this.detalle = detalle;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public String getCodigo() { return codigo; }
    public String getEventoId() { return eventoId; }
    public String getUsuarioId() { return usuarioId; }
    public AccionArchivo getAccion() { return accion; }
    public String getUbicacion() { return ubicacion; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }
}
