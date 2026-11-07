package cl.duoc.tickets.dto;

import cl.duoc.tickets.model.EstadoTicket;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Campos modificables de un ticket. Los que vengan en null no se cambian. */
public record TicketUpdateRequest(
        @Pattern(regexp = "[A-Za-z0-9_-]+") String eventoId,
        String eventoNombre,
        LocalDateTime fechaEvento,
        String lugar,
        @Pattern(regexp = "[A-Za-z0-9_-]+") String usuarioId,
        String usuarioNombre,
        @Email String usuarioEmail,
        String asiento,
        @PositiveOrZero BigDecimal precio,
        EstadoTicket estado
) {
}
