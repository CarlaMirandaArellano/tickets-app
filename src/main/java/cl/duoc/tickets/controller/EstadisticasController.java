package cl.duoc.tickets.controller;

import cl.duoc.tickets.dto.EstadisticasEvento;
import cl.duoc.tickets.service.EstadisticasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 6. Reportar estadísticas de ventas y reservas por evento */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService service;

    public EstadisticasController(EstadisticasService service) {
        this.service = service;
    }

    @GetMapping("/eventos")
    public List<EstadisticasEvento> todos() {
        return service.porTodosLosEventos();
    }

    @GetMapping("/eventos/{eventoId}")
    public EstadisticasEvento porEvento(@PathVariable String eventoId) {
        return service.porEvento(eventoId);
    }
}
