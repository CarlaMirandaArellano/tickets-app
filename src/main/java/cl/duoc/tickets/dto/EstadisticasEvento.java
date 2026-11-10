package cl.duoc.tickets.dto;

import java.math.BigDecimal;

public record EstadisticasEvento(
        String eventoId,
        String eventoNombre,
        long totalTickets,
        long reservados,
        long pagados,
        long cancelados,
        /** Reservas activas = reservados + pagados */
        long reservasActivas,
        /** Suma del precio de los tickets PAGADOS */
        BigDecimal montoVendido,
        /** Suma del precio de los tickets RESERVADOS (aún no pagados) */
        BigDecimal montoReservadoPendiente,
        long usuariosUnicos,
        long archivosEnS3
) {
}
