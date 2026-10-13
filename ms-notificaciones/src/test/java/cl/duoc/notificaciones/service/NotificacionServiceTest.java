package cl.duoc.notificaciones.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cl.duoc.notificaciones.exception.MensajeInvalidoException;
import cl.duoc.notificaciones.messaging.PedidoCreadoEvent;
import org.junit.jupiter.api.Test;

class NotificacionServiceTest {

    private final NotificacionService service = new NotificacionService();

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