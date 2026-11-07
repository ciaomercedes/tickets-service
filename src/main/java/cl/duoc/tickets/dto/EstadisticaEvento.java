package cl.duoc.tickets.dto;

import java.math.BigDecimal;

public record EstadisticaEvento(
        String eventoId,
        String eventoNombre,
        long totalTickets,
        long ticketsReservados,
        long ticketsVendidos,
        long ticketsEliminados,
        long usuariosDistintos,
        BigDecimal ingresosPorVentas,
        BigDecimal montoReservado
) {
}
