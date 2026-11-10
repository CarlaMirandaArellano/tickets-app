package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.ArchivoS3;
import cl.duoc.tickets.model.AccionArchivo;
import cl.duoc.tickets.model.HistorialArchivo;
import cl.duoc.tickets.service.S3StorageService;
import cl.duoc.tickets.service.TicketService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Historial de operaciones sobre archivos y listado real del bucket S3. */
@RestController
@RequestMapping("/api")
public class ArchivosController {

    private final TicketService tickets;
    private final S3StorageService s3;

    public ArchivosController(TicketService tickets, S3StorageService s3) {
        this.tickets = tickets;
        this.s3 = s3;
    }

    /** Historial de acciones: GENERADO_EFS, SUBIDO_S3, MODIFICADO, DESCARGADO, ELIMINADO */
    @GetMapping("/historial")
    public List<HistorialArchivo> historial(@RequestParam(required = false) Long ticketId,
                                            @RequestParam(required = false) String eventoId,
                                            @RequestParam(required = false) String usuarioId,
                                            @RequestParam(required = false) AccionArchivo accion,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return tickets.historial(ticketId, eventoId, usuarioId, accion, desde, hasta);
    }

    /** Lista los objetos que hay en el bucket, por ejemplo ?prefijo=evento123/ */
    @GetMapping("/s3/archivos")
    public List<ArchivoS3> archivosS3(@RequestParam(required = false) String prefijo) {
        return s3.listar(prefijo);
    }
}
