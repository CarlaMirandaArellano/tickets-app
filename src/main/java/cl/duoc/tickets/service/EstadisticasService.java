package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.EstadisticasEvento;
import cl.duoc.tickets.exception.NotFoundException;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Estadísticas de ventas y reservas por evento. */
@Service
public class EstadisticasService {

    private final TicketRepository tickets;

    public EstadisticasService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    public List<EstadisticasEvento> porTodosLosEventos() {
        Map<String, List<Ticket>> porEvento = tickets.findAll().stream()
                .collect(Collectors.groupingBy(Ticket::getEventoId, TreeMap::new, Collectors.toList()));
        return porEvento.entrySet().stream()
                .map(e -> calcular(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(EstadisticasEvento::eventoId))
                .toList();
    }

    public EstadisticasEvento porEvento(String eventoId) {
        List<Ticket> lista = tickets.findByEventoId(eventoId);
        if (lista.isEmpty()) {
            throw new NotFoundException("No hay tickets para el evento " + eventoId);
        }
        return calcular(eventoId, lista);
    }

    private EstadisticasEvento calcular(String eventoId, List<Ticket> lista) {
        long reservados = contar(lista, EstadoTicket.RESERVADO);
        long pagados = contar(lista, EstadoTicket.PAGADO);
        long cancelados = contar(lista, EstadoTicket.CANCELADO);
        return new EstadisticasEvento(
                eventoId,
                lista.get(0).getEventoNombre(),
                lista.size(),
                reservados,
                pagados,
                cancelados,
                reservados + pagados,
                sumar(lista, EstadoTicket.PAGADO),
                sumar(lista, EstadoTicket.RESERVADO),
                lista.stream().map(Ticket::getUsuarioId).distinct().count(),
                lista.stream().filter(Ticket::isSubidoS3).count()
        );
    }

    private long contar(List<Ticket> lista, EstadoTicket estado) {
        return lista.stream().filter(t -> t.getEstado() == estado).count();
    }

    private BigDecimal sumar(List<Ticket> lista, EstadoTicket estado) {
        return lista.stream()
                .filter(t -> t.getEstado() == estado && t.getPrecio() != null)
                .map(Ticket::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
