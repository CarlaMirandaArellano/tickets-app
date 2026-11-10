package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    /** 1. Generar ticket (PDF en EFS + subida automática a S3) */
    @PostMapping
    public ResponseEntity<Ticket> generar(@Valid @RequestBody TicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generar(req));
    }

    /** 2a. Subir (o volver a subir) un ticket a S3 */
    @PostMapping("/{id}/subir")
    public Ticket subir(@PathVariable Long id) {
        return service.subirAS3(id);
    }

    /** 2b. Subir a S3 todos los tickets pendientes */
    @PostMapping("/subir-pendientes")
    public Map<String, Object> subirPendientes() {
        List<Ticket> subidos = service.subirPendientes();
        return Map.of("subidos", subidos.size(), "tickets", subidos);
    }

    /** Historial de archivos generados con filtros */
    @GetMapping
    public List<Ticket> listar(@RequestParam(required = false) String eventoId,
                               @RequestParam(required = false) String usuarioId,
                               @RequestParam(required = false) EstadoTicket estado,
                               @RequestParam(required = false) Boolean subidoS3,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(eventoId, usuarioId, estado, subidoS3, desde, hasta);
    }

    @GetMapping("/{id}")
    public Ticket obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** 3a. Descargar un ticket (PDF) desde S3 */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Ticket t = service.obtener(id);
        byte[] pdf = service.descargar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(t.getCodigo() + ".pdf").build().toString())
                .body(pdf);
    }

    /** 3b. Descargar en ZIP los tickets de un usuario y/o de un evento desde S3 */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarZip(@RequestParam(required = false) String eventoId,
                                               @RequestParam(required = false) String usuarioId) {
        byte[] zip = service.descargarZip(eventoId, usuarioId);
        String nombre = "tickets"
                + (eventoId != null ? "_" + eventoId : "")
                + (usuarioId != null ? "_" + usuarioId : "")
                + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .body(zip);
    }

    /** 4. Modificar detalles del ticket (regenera el PDF y lo reemplaza en S3) */
    @PutMapping("/{id}")
    public Ticket actualizar(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest req) {
        return service.actualizar(id, req);
    }

    /** 5. Eliminar un ticket específico (S3 + EFS + BD) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Ticket " + id + " eliminado de S3, EFS y base de datos"));
    }
}
