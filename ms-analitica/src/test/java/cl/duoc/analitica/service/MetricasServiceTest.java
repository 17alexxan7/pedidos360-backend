package cl.duoc.analitica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.duoc.analitica.model.MetricasResumen;
import cl.duoc.analitica.model.PedidoEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricasServiceTest {

    private MetricasService service;

    @BeforeEach
    void setUp() {
        service = new MetricasService();
    }

    private PedidoEvento evento(String id, long pedidoId, String producto, int cantidad, String estado) {
        return new PedidoEvento(id, "PEDIDO_CREADO", pedidoId, "Ana", producto, cantidad, estado, "2026-11-13T04:00:00Z");
    }

    @Test
    void resumenInicialEstaVacio() {
        MetricasResumen r = service.resumen();

        assertEquals(0, r.pedidosTotales());
        assertEquals(0, r.unidadesAcumuladas());
        assertTrue(r.pedidosPorEstado().isEmpty());
        assertTrue(r.pedidosPorProducto().isEmpty());
        assertNull(r.actualizadoEn());
    }

    @Test
    void registrarCuentaPedidosYUnidades() {
        assertTrue(service.registrar(evento("e1", 1, "Mouse", 1, "PENDIENTE")));
        assertTrue(service.registrar(evento("e2", 2, "Teclado", 2, "PENDIENTE")));
        assertTrue(service.registrar(evento("e3", 3, "Teclado", 3, "PENDIENTE")));

        MetricasResumen r = service.resumen();
        assertEquals(3, r.pedidosTotales());
        assertEquals(6, r.unidadesAcumuladas());
        assertNotNull(r.actualizadoEn());
    }

    @Test
    void eventIdRepetidoSeDescartaYNoAlteraLasMetricas() {
        assertTrue(service.registrar(evento("e1", 1, "Mouse", 2, "PENDIENTE")));
        assertFalse(service.registrar(evento("e1", 1, "Mouse", 2, "PENDIENTE")));

        MetricasResumen r = service.resumen();
        assertEquals(1, r.pedidosTotales());
        assertEquals(2, r.unidadesAcumuladas());
        assertEquals(1L, r.pedidosPorProducto().get("Mouse"));
    }

    @Test
    void agrupaPorEstadoYPorProducto() {
        service.registrar(evento("e1", 1, "Mouse", 1, "PENDIENTE"));
        service.registrar(evento("e2", 2, "Teclado", 1, "PENDIENTE"));
        service.registrar(evento("e3", 3, "Teclado", 1, "ENVIADO"));

        MetricasResumen r = service.resumen();
        assertEquals(2L, r.pedidosPorEstado().get("PENDIENTE"));
        assertEquals(1L, r.pedidosPorEstado().get("ENVIADO"));
        assertEquals(1L, r.pedidosPorProducto().get("Mouse"));
        assertEquals(2L, r.pedidosPorProducto().get("Teclado"));
    }
}