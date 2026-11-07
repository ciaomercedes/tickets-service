package cl.duoc.tickets.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventoId;

    @Column(nullable = false)
    private String eventoNombre;

    private LocalDateTime fechaEvento;

    private String lugar;

    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false)
    private String usuarioNombre;

    private String usuarioEmail;

    private String asiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTicket estado;

    /** Nombre del archivo, ej: ticket001.pdf */
    private String nombreArchivo;

    /** Ruta del PDF en el EFS (almacenamiento temporal). */
    private String rutaEfs;

    /** Clave del objeto en S3, ej: evento123/usuarioX/ticket001.pdf */
    private String claveS3;

    private boolean subidoS3;

    /** Cuántas veces se ha generado el PDF (sube con cada modificación). */
    private int version;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaSubidaS3;
    private LocalDateTime fechaEliminacion;

    // ---------- getters y setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getAsiento() { return asiento; }
    public void setAsiento(String asiento) { this.asiento = asiento; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getRutaEfs() { return rutaEfs; }
    public void setRutaEfs(String rutaEfs) { this.rutaEfs = rutaEfs; }

    public String getClaveS3() { return claveS3; }
    public void setClaveS3(String claveS3) { this.claveS3 = claveS3; }

    public boolean isSubidoS3() { return subidoS3; }
    public void setSubidoS3(boolean subidoS3) { this.subidoS3 = subidoS3; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaSubidaS3() { return fechaSubidaS3; }
    public void setFechaSubidaS3(LocalDateTime fechaSubidaS3) { this.fechaSubidaS3 = fechaSubidaS3; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }
}
