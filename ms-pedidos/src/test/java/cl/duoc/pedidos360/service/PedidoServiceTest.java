package cl.duoc.pedidos360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.duoc.pedidos360.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoServiceTest {

    private PedidoRepository repository;
    private PedidoEventPublisher publisher;
    private PedidoService service;

    @BeforeEach
    void setUp() {
        repository = mock(PedidoRepository.class);
        publisher = mock(PedidoEventPublisher.class);
        when(repository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new PedidoService(repository, publisher);
    }

    @Test
    void siembraLosPedidosDeEjemploSoloSiLaTablaEstaVaciaYNoPublica() {
        when(repository.count()).thenReturn(0L);

        service.sembrarDatosDeEjemplo();

        verify(repository, times(2)).save(any(Pedido.class));
        verifyNoInteractions(publisher);
    }

    @Test
    void noSiembraSiYaHayPedidos() {
        when(repository.count()).thenReturn(2L);

        service.sembrarDatosDeEjemplo();

        verify(repository, never()).save(any(Pedido.class));
    }

    @Test
    void crearDejaEstadoPendienteIgnoraElIdRecibidoYPublicaUnaVez() {
        Pedido creado = service.crear(new Pedido(99L, "Prueba", "Teclado", 1, ""));

        assertNull(creado.getId());
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
        when(repository.findById(9999L)).thenReturn(Optional.empty());

        assertTrue(service.buscarPorId(9999L).isEmpty());
    }

    @Test
    void actualizarInexistenteDevuelveVacio() {
        when(repository.findById(9999L)).thenReturn(Optional.empty());

        assertTrue(service.actualizar(9999L, new Pedido(null, "X", "Y", 1, "NUEVO")).isEmpty());
        verify(repository, never()).save(any(Pedido.class));
    }

    @Test
    void actualizarExistenteCopiaLosCamposYGuarda() {
        Pedido existente = new Pedido(5L, "Ana", "Notebook", 1, "PENDIENTE");
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        Pedido resultado = service.actualizar(5L, new Pedido(null, "Luis", "Mouse", 3, "ENVIADO")).orElseThrow();

        assertEquals("Luis", resultado.getCliente());
        assertEquals(3, resultado.getCantidad());
        assertEquals("ENVIADO", resultado.getEstado());
        verify(repository).save(existente);
    }

    @Test
    void eliminarExistenteBorraYDevuelveTrue() {
        when(repository.existsById(1L)).thenReturn(true);

        assertTrue(service.eliminar(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void eliminarInexistenteDevuelveFalseSinBorrar() {
        when(repository.existsById(2L)).thenReturn(false);

        assertFalse(service.eliminar(2L));
        verify(repository, never()).deleteById(2L);
    }
}