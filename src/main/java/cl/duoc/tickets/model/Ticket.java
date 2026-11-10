package cl.duoc.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código del archivo, por ejemplo ticket001 */
    @Column(unique = true)
    private String codigo;

    @Column(nullable = false)
    private String eventoId;
    private String eventoNombre;
    private LocalDateTime fechaEvento;
    private String lugar;

    @Column(nullable = false)
    private String usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;

    private String tipoEntrada;
    private String asiento;
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTicket estado;

    /** Ruta del PDF en el EFS (almacenamiento temporal) */
    private String rutaEfs;
    /** Key del PDF en S3: evento/usuario/ticketXXX.pdf */
    private String s3Key;
    private boolean subidoS3;
    private LocalDateTime fechaSubidaS3;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }
    public String getEventoNombre() { return eventoNombre; }
    public void setEventoNombre(String eventoNombre) { this.eventoNombre = eventoNombre; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }
    public String getTipoEntrada() { return tipoEntrada; }
    public void setTipoEntrada(String tipoEntrada) { this.tipoEntrada = tipoEntrada; }
    public String getAsiento() { return asiento; }
    public void setAsiento(String asiento) { this.asiento = asiento; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }
    public String getRutaEfs() { return rutaEfs; }
    public void setRutaEfs(String rutaEfs) { this.rutaEfs = rutaEfs; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public boolean isSubidoS3() { return subidoS3; }
    public void setSubidoS3(boolean subidoS3) { this.subidoS3 = subidoS3; }
    public LocalDateTime getFechaSubidaS3() { return fechaSubidaS3; }
    public void setFechaSubidaS3(LocalDateTime fechaSubidaS3) { this.fechaSubidaS3 = fechaSubidaS3; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
