package cl.duoc.tickets.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.tickets.exception.MensajeInvalidoException;
import cl.duoc.tickets.messaging.PedidoCreadoEvent;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    private final TicketService service = new TicketService();

    private PedidoCreadoEvent evento(String cliente, String producto, int cantidad) {
        return new PedidoCreadoEvent("evt-1", 1L, cliente, producto, cantidad, "PENDIENTE", "2026-10-13T20:00:00");
    }

    @Test
    void pedidoValidoSeProcesaSinErrores() {
        assertDoesNotThrow(() -> service.enviar(evento("Ana", "Teclado", 1)));
    }

    @Test
    void clienteVacioLanzaMensajeInvalido() {
        assertThrows(MensajeInvalidoException.class, () -> service.enviar(evento("", "Teclado", 1)));
    }

    @Test
    void cantidadCeroLanzaMensajeInvalido() {
        assertThrows(MensajeInvalidoException.class, () -> service.enviar(evento("Ana", "Teclado", 0)));
    }

    @Test
    void productoFallaTemporalLanzaErrorRecuperable() {
        assertThrows(IllegalStateException.class, () -> service.enviar(evento("Ana", "FALLA-TEMPORAL-1", 1)));
    }
}