package cl.duoc.tickets.repository;

import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByEstadoNot(EstadoTicket estado);

    List<Ticket> findByEventoIdAndEstadoNot(String eventoId, EstadoTicket estado);
}
