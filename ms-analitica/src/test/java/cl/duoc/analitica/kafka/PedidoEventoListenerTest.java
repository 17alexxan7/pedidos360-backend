package cl.duoc.analitica.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.analitica.config.KafkaProperties;
import cl.duoc.analitica.exception.EventoInvalidoException;
import cl.duoc.analitica.model.MetricasResumen;
import cl.duoc.analitica.model.PedidoEvento;
import cl.duoc.analitica.service.MetricasService;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class PedidoEventoListenerTest {

    private static final String VALIDO = "{\"eventId\":\"e1\",\"tipo\":\"PEDIDO_CREADO\",\"pedidoId\":7,"
            + "\"cliente\":\"Ana\",\"producto\":\"Mouse\",\"cantidad\":3,\"estado\":\"PENDIENTE\","
            + "\"occurredAt\":\"2026-11-13T04:00:00Z\"}";

    private MetricasService metricas;
    private MetricasPublisher publisher;
    private PedidoEventoListener listener;

    @BeforeEach
    void setUp() {
        KafkaProperties props = mock(KafkaProperties.class);
        KafkaProperties.Events events = mock(KafkaProperties.Events.class);
        when(events.pedidoCreado()).thenReturn("PEDIDO_CREADO");
        when(props.events()).thenReturn(events);

        metricas = mock(MetricasService.class);
        publisher = mock(MetricasPublisher.class);
        listener = new PedidoEventoListener(JsonMapper.builder().build(), props, metricas, publisher);
    }

    private ConsumerRecord<String, String> record(String valor) {
        return new ConsumerRecord<>("pedidos.eventos", 0, 0L, "7", valor);
    }

    @Test
    void eventoValidoRegistraYPublicaElResumen() {
        MetricasResumen resumen = new MetricasResumen(1, 3, Map.of("PENDIENTE", 1L), Map.of("Mouse", 1L), null);
        when(metricas.registrar(any(PedidoEvento.class))).thenReturn(true);
        when(metricas.resumen()).thenReturn(resumen);

        listener.consumir(record(VALIDO));

        ArgumentCaptor<PedidoEvento> captor = ArgumentCaptor.forClass(PedidoEvento.class);
        verify(metricas).registrar(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("e1", captor.getValue().eventId());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().cantidad());
        verify(publisher).publicar(resumen);
    }

    @Test
    void eventoRepetidoNoRepublica() {
        when(metricas.registrar(any(PedidoEvento.class))).thenReturn(false);

        listener.consumir(record(VALIDO));

        verify(metricas, times(1)).registrar(any(PedidoEvento.class));
        verify(publisher, never()).publicar(any());
    }

    @Test
    void jsonRotoLanzaEventoInvalido() {
        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record("JSON")));
        verify(metricas, never()).registrar(any());
        verify(publisher, never()).publicar(any());
    }

    @Test
    void mensajeVacioONuloLanzaEventoInvalido() {
        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record("   ")));
        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record(null)));
        verify(metricas, never()).registrar(any());
    }

    @Test
    void tipoDesconocidoLanzaEventoInvalido() {
        String json = VALIDO.replace("PEDIDO_CREADO", "PEDIDO_BORRADO");

        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record(json)));
        verify(metricas, never()).registrar(any());
        verify(publisher, never()).publicar(any());
    }

    @Test
    void camposFaltantesOInvalidosLanzanEventoInvalido() {
        String sinEventId = VALIDO.replace("\"eventId\":\"e1\",", "");
        String sinProducto = VALIDO.replace("\"producto\":\"Mouse\",", "");
        String cantidadCero = VALIDO.replace("\"cantidad\":3", "\"cantidad\":0");

        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record(sinEventId)));
        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record(sinProducto)));
        assertThrows(EventoInvalidoException.class, () -> listener.consumir(record(cantidadCero)));
        verify(metricas, never()).registrar(any());
    }
}