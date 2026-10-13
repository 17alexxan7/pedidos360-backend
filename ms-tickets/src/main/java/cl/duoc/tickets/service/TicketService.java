package cl.duoc.tickets.service;

import cl.duoc.tickets.exception.MensajeInvalidoException;
import cl.duoc.tickets.messaging.PedidoCreadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de tickets. No conoce RabbitMQ: solo recibe el
 * evento y lanza excepciones que el listener interpreta.
 * - MensajeInvalidoException: error irrecuperable (datos invalidos).
 * - Cualquier otra RuntimeException: error recuperable (falla temporal).
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    public void enviar(PedidoCreadoEvent evento) {
        if (evento.cliente() == null || evento.cliente().isBlank() || evento.cantidad() <= 0) {
            throw new MensajeInvalidoException("Pedido " + evento.pedidoId() + " con cliente vacio o cantidad invalida");
        }
        // Simulacion de falla temporal para la demo (servicio de correo caido).
        if (evento.producto() != null && evento.producto().startsWith("FALLA-TEMPORAL")) {
            throw new IllegalStateException("Servicio de tickets no disponible");
        }
        log.info("Ticket procesado para '{}' por pedido {} (eventId={})",
                evento.cliente(), evento.pedidoId(), evento.eventId());
    }
}