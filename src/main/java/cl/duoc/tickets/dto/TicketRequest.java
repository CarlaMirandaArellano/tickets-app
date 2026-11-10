package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos para generar un ticket nuevo. */
public record TicketRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, números, guion y guion bajo")
        String eventoId,
        @NotBlank String eventoNombre,
        LocalDateTime fechaEvento,
        String lugar,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, números, guion y guion bajo")
        String usuarioId,
        @NotBlank String usuarioNombre,
        @Email String usuarioEmail,
        String tipoEntrada,
        String asiento,
        @NotNull @PositiveOrZero BigDecimal precio,
        /** Opcional: si no viene, el ticket queda RESERVADO */
        EstadoTicket estado
) {
}
