package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos para generar uno o más tickets de un usuario para un evento. */
public record TicketRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]+", message = "solo letras, números, '-' y '_'") String eventoId,
        @NotBlank String eventoNombre,
        LocalDateTime fechaEvento,
        String lugar,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]+", message = "solo letras, números, '-' y '_'") String usuarioId,
        @NotBlank String usuarioNombre,
        @Email String usuarioEmail,
        String asiento,
        @NotNull @PositiveOrZero BigDecimal precio,
        /** RESERVADO o VENDIDO. Si no se envía, queda RESERVADO. */
        EstadoTicket estado,
        /** Cantidad de tickets a generar (1 a 10). Si no se envía, 1. */
        @Min(1) @Max(10) Integer cantidad
) {
}
