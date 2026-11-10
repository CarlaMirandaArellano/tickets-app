package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Todos los campos son opcionales: solo se cambian los que vienen. */
public record TicketUpdateRequest(
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, números, guion y guion bajo")
        String eventoId,
        String eventoNombre,
        LocalDateTime fechaEvento,
        String lugar,
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, números, guion y guion bajo")
        String usuarioId,
        String usuarioNombre,
        @Email String usuarioEmail,
        String tipoEntrada,
        String asiento,
        @PositiveOrZero BigDecimal precio,
        EstadoTicket estado
) {
}
