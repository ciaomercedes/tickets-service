package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.exception.RecursoNoEncontradoException;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Lógica de negocio de los tickets.
 *
 * Flujo: generar PDF -> guardar en EFS (temporal) -> subir a S3 en {eventoId}/{usuarioId}/ticketNNN.pdf
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository repo;
    private final TicketPdfGenerator pdf;
    private final EfsStorageService efs;
    private final S3StorageService s3;
    private final boolean subidaAutomatica;

    public TicketService(TicketRepository repo, TicketPdfGenerator pdf, EfsStorageService efs, S3StorageService s3,
                         @Value("${app.s3.auto-upload}") boolean subidaAutomatica) {
        this.repo = repo;
        this.pdf = pdf;
        this.efs = efs;
        this.s3 = s3;
        this.subidaAutomatica = subidaAutomatica;
    }

    // ------------------------------------------------------------------ generar

    @Transactional
    public List<Ticket> generar(TicketRequest req) {
        int cantidad = req.cantidad() == null ? 1 : req.cantidad();
        EstadoTicket estado = req.estado() == null ? EstadoTicket.RESERVADO : req.estado();
        if (estado == EstadoTicket.ELIMINADO) {
            throw new IllegalArgumentException("Un ticket nuevo solo puede ser RESERVADO o VENDIDO");
        }

        List<Ticket> generados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Ticket t = new Ticket();
            t.setEventoId(req.eventoId());
            t.setEventoNombre(req.eventoNombre());
            t.setFechaEvento(req.fechaEvento());
            t.setLugar(req.lugar());
            t.setUsuarioId(req.usuarioId());
            t.setUsuarioNombre(req.usuarioNombre());
            t.setUsuarioEmail(req.usuarioEmail());
            t.setAsiento(req.asiento());
            t.setPrecio(req.precio());
            t.setEstado(estado);
            t.setFechaCreacion(LocalDateTime.now());
            t.setFechaActualizacion(t.getFechaCreacion());
            t = repo.save(t); // para obtener el id con que se nombra el archivo

            t.setNombreArchivo(String.format("ticket%03d.pdf", t.getId()));
            escribirEnEfs(t);
            if (subidaAutomatica) {
                intentarSubir(t);
            }
            generados.add(repo.save(t));
        }
        return generados;
    }

    // ------------------------------------------------------------------ subir a S3

    @Transactional
    public Ticket subirAS3(Long id) {
        Ticket t = buscarActivo(id);
        subir(t);
        return repo.save(t);
    }

    /** Sube a S3 todos los tickets que quedaron solo en el EFS (por ejemplo si S3 falló). */
    @Transactional
    public List<Ticket> subirPendientes() {
        List<Ticket> pendientes = repo.findByEstadoNot(EstadoTicket.ELIMINADO).stream()
                .filter(t -> !t.isSubidoS3())
                .toList();
        pendientes.forEach(this::subir);
        return repo.saveAll(pendientes);
    }

    // ------------------------------------------------------------------ modificar

    @Transactional
    public Ticket modificar(Long id, TicketUpdateRequest req) {
        Ticket t = buscarActivo(id);
        if (req.estado() == EstadoTicket.ELIMINADO) {
            throw new IllegalArgumentException("Para eliminar un ticket use DELETE /api/tickets/{id}");
        }
        String claveAnterior = t.getClaveS3();
        boolean estabaEnS3 = t.isSubidoS3();
        String rutaEfsAnterior = t.getRutaEfs();

        if (req.eventoId() != null) t.setEventoId(req.eventoId());
        if (req.eventoNombre() != null) t.setEventoNombre(req.eventoNombre());
        if (req.fechaEvento() != null) t.setFechaEvento(req.fechaEvento());
        if (req.lugar() != null) t.setLugar(req.lugar());
        if (req.usuarioId() != null) t.setUsuarioId(req.usuarioId());
        if (req.usuarioNombre() != null) t.setUsuarioNombre(req.usuarioNombre());
        if (req.usuarioEmail() != null) t.setUsuarioEmail(req.usuarioEmail());
        if (req.asiento() != null) t.setAsiento(req.asiento());
        if (req.precio() != null) t.setPrecio(req.precio());
        if (req.estado() != null) t.setEstado(req.estado());
        t.setFechaActualizacion(LocalDateTime.now());

        // 1) Se regenera el PDF con los datos nuevos en el EFS
        escribirEnEfs(t);
        if (rutaEfsAnterior != null && !rutaEfsAnterior.equals(t.getRutaEfs())) {
            efs.eliminar(rutaEfsAnterior);
        }

        // 2) Se actualiza en S3: misma clave = se sobrescribe; si cambió evento/usuario, se mueve
        if (estabaEnS3 || subidaAutomatica) {
            subir(t);
            if (estabaEnS3 && claveAnterior != null && !claveAnterior.equals(t.getClaveS3())) {
                s3.eliminar(claveAnterior);
            }
        }
        return repo.save(t);
    }

    // ------------------------------------------------------------------ eliminar

    /** Borra el PDF del EFS y de S3. El registro queda en el historial con estado ELIMINADO. */
    @Transactional
    public Ticket eliminar(Long id) {
        Ticket t = buscarActivo(id);
        if (t.isSubidoS3() && t.getClaveS3() != null) {
            s3.eliminar(t.getClaveS3());
        }
        efs.eliminar(t.getRutaEfs());
        t.setSubidoS3(false);
        t.setEstado(EstadoTicket.ELIMINADO);
        t.setFechaEliminacion(LocalDateTime.now());
        t.setFechaActualizacion(t.getFechaEliminacion());
        return repo.save(t);
    }

    // ------------------------------------------------------------------ descargar

    public Ticket buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el ticket " + id));
    }

    public byte[] descargarDesdeS3(Long id) {
        Ticket t = buscarActivo(id);
        if (!t.isSubidoS3()) {
            throw new IllegalStateException("El ticket " + id + " aún no está en S3. Súbalo con POST /api/tickets/" + id + "/subir");
        }
        return s3.descargar(t.getClaveS3());
    }

    /** Descarga desde S3, en un ZIP, todos los tickets de un usuario y/o de un evento. */
    public byte[] descargarZip(String eventoId, String usuarioId) {
        if (isBlank(eventoId) && isBlank(usuarioId)) {
            throw new IllegalArgumentException("Debe indicar eventoId y/o usuarioId");
        }
        List<Ticket> tickets = historial(eventoId, usuarioId, null, null, null).stream()
                .filter(t -> t.getEstado() != EstadoTicket.ELIMINADO && t.isSubidoS3())
                .toList();
        if (tickets.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay tickets en S3 para ese filtro");
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Ticket t : tickets) {
                zip.putNextEntry(new ZipEntry(t.getClaveS3())); // conserva carpetas evento/usuario
                zip.write(s3.descargar(t.getClaveS3()));
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ historial

    /** Historial de tickets generados con filtros opcionales (todos combinables). */
    public List<Ticket> historial(String eventoId, String usuarioId, EstadoTicket estado, LocalDate desde, LocalDate hasta) {
        Specification<Ticket> spec = (root, q, cb) -> cb.conjunction();
        if (!isBlank(eventoId)) spec = spec.and((r, q, cb) -> cb.equal(r.get("eventoId"), eventoId));
        if (!isBlank(usuarioId)) spec = spec.and((r, q, cb) -> cb.equal(r.get("usuarioId"), usuarioId));
        if (estado != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("estado"), estado));
        if (desde != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("fechaCreacion"), desde.atStartOfDay()));
        if (hasta != null) spec = spec.and((r, q, cb) -> cb.lessThan(r.get("fechaCreacion"), hasta.plusDays(1).atStartOfDay()));
        return repo.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    // ------------------------------------------------------------------ auxiliares

    private Ticket buscarActivo(Long id) {
        Ticket t = buscar(id);
        if (t.getEstado() == EstadoTicket.ELIMINADO) {
            throw new RecursoNoEncontradoException("El ticket " + id + " fue eliminado");
        }
        return t;
    }

    private void escribirEnEfs(Ticket t) {
        t.setVersion(t.getVersion() + 1);
        byte[] contenido = pdf.generar(t);
        t.setRutaEfs(efs.guardar(t.getEventoId(), t.getUsuarioId(), t.getNombreArchivo(), contenido));
        t.setClaveS3(t.getEventoId() + "/" + t.getUsuarioId() + "/" + t.getNombreArchivo());
    }

    private void subir(Ticket t) {
        if (!efs.existe(t.getRutaEfs())) {
            escribirEnEfs(t); // si el archivo temporal ya no está, se vuelve a generar
        }
        s3.subir(t.getClaveS3(), efs.leer(t.getRutaEfs()), Map.of(
                "ticket-id", String.valueOf(t.getId()),
                "evento-id", t.getEventoId(),
                "usuario-id", t.getUsuarioId(),
                "estado", t.getEstado().name(),
                "version", String.valueOf(t.getVersion())));
        t.setSubidoS3(true);
        t.setFechaSubidaS3(LocalDateTime.now());
        log.info("Ticket {} subido a s3://{}/{}", t.getId(), s3.getBucket(), t.getClaveS3());
    }

    /** En la generación, si S3 falla el ticket igual queda guardado en el EFS (subidoS3 = false). */
    private void intentarSubir(Ticket t) {
        try {
            subir(t);
        } catch (RuntimeException e) {
            log.warn("No se pudo subir el ticket {} a S3, queda pendiente en el EFS: {}", t.getId(), e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
