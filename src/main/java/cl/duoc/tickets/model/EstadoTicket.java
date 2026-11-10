package cl.duoc.tickets.model;

public enum EstadoTicket {
    /** Reservado pero aún no pagado */
    RESERVADO,
    /** Pagado: cuenta como venta */
    PAGADO,
    /** Cancelado por el usuario */
    CANCELADO
}
