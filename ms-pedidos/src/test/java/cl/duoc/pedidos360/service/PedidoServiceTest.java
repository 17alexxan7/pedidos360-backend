package cl.duoc.pedidos360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cl.duoc.pedidos360.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoServiceTest {

    private PedidoEventPublisher publisher;
    private PedidoService service;

    @BeforeEach
    void setUp() {
        publisher = mock(PedidoEventPublisher.class);
        service = new PedidoService(publisher);
    }

    @Test
    void losPedidosDeEjemploNoPublicanEventos() {
        assertEquals(2, service.listarTodos().size());
        verifyNoInteractions(publisher);
    }

    @Test
    void crearAsignaIdEstadoPorDefectoYPublicaUnaVez() {
        Pedido creado = service.crear(new Pedido(null, "Prueba", "Teclado", 1, ""));

        assertNotNull(creado.getId());
        assertEquals("PENDIENTE", creado.getEstado());
        verify(publisher, times(1)).publicarPedidoCreado(creado);
    }

    @Test
    void crearRespetaElEstadoIndicado() {
        Pedido creado = service.crear(new Pedido(null, "Prueba", "Teclado", 1, "ENVIADO"));

        assertEquals("ENVIADO", creado.getEstado());
    }

    @Test
    void buscarPorIdInexistenteDevuelveVacio() {
        assertTrue(service.buscarPorId(9999L).isEmpty());
    }

    @Test
    void actualizarInexistenteDevuelveVacio() {
        assertTrue(service.actualizar(9999L, new Pedido(null, "X", "Y", 1, "NUEVO")).isEmpty());
    }

    @Test
    void eliminarPedidoExistenteDevuelveTrue() {
        Pedido creado = service.crear(new Pedido(null, "Prueba", "Mouse", 1, "NUEVO"));

        assertTrue(service.eliminar(creado.getId()));
        assertFalse(service.eliminar(creado.getId()));
    }
}