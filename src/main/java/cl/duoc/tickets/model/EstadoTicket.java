package cl.duoc.tickets.model;

/** RESERVADO: ticket apartado sin pagar. VENDIDO: ticket pagado. ELIMINADO: ticket borrado (queda en el historial). */
public enum EstadoTicket {
    RESERVADO,
    VENDIDO,
    ELIMINADO
}
