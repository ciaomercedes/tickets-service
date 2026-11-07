package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.EstadisticaEvento;
import cl.duoc.tickets.exception.RecursoNoEncontradoException;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Estadísticas de ventas y reservas por evento. */
@Service
public class EstadisticaService {

    private final TicketRepository repo;

    public EstadisticaService(TicketRepository repo) {
        this.repo = repo;
    }

    public List<EstadisticaEvento> porEvento() {
        Map<String, List<Ticket>> porEvento = repo.findAll().stream()
                .collect(Collectors.groupingBy(Ticket::getEventoId));
        return porEvento.entrySet().stream()
                .map(e -> calcular(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(EstadisticaEvento::ingresosPorVentas).reversed())
                .toList();
    }

    public EstadisticaEvento deEvento(String eventoId) {
        List<Ticket> tickets = repo.findAll().stream().filter(t -> t.getEventoId().equals(eventoId)).toList();
        if (tickets.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay tickets para el evento " + eventoId);
        }
        return calcular(eventoId, tickets);
    }

    private EstadisticaEvento calcular(String eventoId, List<Ticket> tickets) {
        List<Ticket> activos = tickets.stream().filter(t -> t.getEstado() != EstadoTicket.ELIMINADO).toList();
        List<Ticket> vendidos = activos.stream().filter(t -> t.getEstado() == EstadoTicket.VENDIDO).toList();
        List<Ticket> reservados = activos.stream().filter(t -> t.getEstado() == EstadoTicket.RESERVADO).toList();
        String nombre = tickets.stream().max(Comparator.comparing(Ticket::getFechaActualizacion))
                .map(Ticket::getEventoNombre).orElse(eventoId);

        return new EstadisticaEvento(
                eventoId,
                nombre,
                activos.size(),
                reservados.size(),
                vendidos.size(),
                tickets.size() - activos.size(),
                activos.stream().map(Ticket::getUsuarioId).distinct().count(),
                suma(vendidos),
                suma(reservados));
    }

    private static BigDecimal suma(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
