package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.ArchivoS3;
import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.service.S3StorageService;
import cl.duoc.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;
    private final S3StorageService s3;

    public TicketController(TicketService service, S3StorageService s3) {
        this.service = service;
        this.s3 = s3;
    }

    /** 1. Generar tickets: crea el PDF en el EFS y (si app.s3.auto-upload=true) lo sube a S3. */
    @PostMapping
    public ResponseEntity<List<Ticket>> generar(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generar(request));
    }

    /** 2. Subir (o volver a subir) un ticket a S3 desde el EFS. */
    @PostMapping("/{id}/subir")
    public Ticket subir(@PathVariable Long id) {
        return service.subirAS3(id);
    }

    /** 2b. Subir a S3 todos los tickets que quedaron pendientes en el EFS. */
    @PostMapping("/subir-pendientes")
    public List<Ticket> subirPendientes() {
        return service.subirPendientes();
    }

    /** 3a. Descargar un ticket (PDF) desde S3. */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Ticket t = service.buscar(id);
        byte[] contenido = service.descargarDesdeS3(id);
        return archivo(contenido, t.getEventoId() + "_" + t.getUsuarioId() + "_" + t.getNombreArchivo(), MediaType.APPLICATION_PDF);
    }

    /** 3b. Descargar desde S3 todos los tickets de un usuario y/o evento, en un ZIP. */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarZip(@RequestParam(required = false) String eventoId,
                                               @RequestParam(required = false) String usuarioId) {
        String nombre = "tickets"
                + (eventoId != null ? "_" + eventoId : "")
                + (usuarioId != null ? "_" + usuarioId : "") + ".zip";
        return archivo(service.descargarZip(eventoId, usuarioId), nombre, MediaType.parseMediaType("application/zip"));
    }

    /** 4. Modificar detalles del ticket: regenera el PDF en el EFS y lo actualiza en S3. */
    @PutMapping("/{id}")
    public Ticket modificar(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
        return service.modificar(id, request);
    }

    /** 5. Eliminar un ticket específico (borra el PDF de EFS y S3; queda en el historial como ELIMINADO). */
    @DeleteMapping("/{id}")
    public Ticket eliminar(@PathVariable Long id) {
        return service.eliminar(id);
    }

    /** Historial de tickets generados, con filtros opcionales combinables. */
    @GetMapping
    public List<Ticket> historial(@RequestParam(required = false) String eventoId,
                                  @RequestParam(required = false) String usuarioId,
                                  @RequestParam(required = false) EstadoTicket estado,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.historial(eventoId, usuarioId, estado, desde, hasta);
    }

    @GetMapping("/{id}")
    public Ticket obtener(@PathVariable Long id) {
        return service.buscar(id);
    }

    /** Lista lo que hay realmente en el bucket (para mostrar la organización evento/usuario). */
    @GetMapping("/s3")
    public List<ArchivoS3> listarS3(@RequestParam(required = false) String prefijo) {
        return s3.listar(prefijo);
    }

    private static ResponseEntity<byte[]> archivo(byte[] contenido, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}
