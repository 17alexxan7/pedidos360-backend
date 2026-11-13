package cl.duoc.analitica.kafka;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.analitica.config.KafkaProperties;
import cl.duoc.analitica.model.MetricasResumen;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

class MetricasPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private MetricasPublisher publisher;
    private MetricasResumen resumen;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);

        KafkaProperties props = mock(KafkaProperties.class);
        KafkaProperties.Topics topics = mock(KafkaProperties.Topics.class);
        when(topics.metricas()).thenReturn("pedidos.metricas");
        when(props.topics()).thenReturn(topics);

        publisher = new MetricasPublisher(kafkaTemplate, JsonMapper.builder().build(), props);
        resumen = new MetricasResumen(3, 6, Map.of("PENDIENTE", 3L), Map.of("Mouse", 1L, "Teclado", 2L),
                Instant.parse("2026-11-13T04:08:50Z"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicaEnElTopicoDeMetricasConClaveGlobal() {
        SendResult<String, String> resultado = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(resultado.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("pedidos.metricas"), eq("global"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(resultado));

        publisher.publicar(resumen);

        ArgumentCaptor<String> valor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("pedidos.metricas"), eq("global"), valor.capture());
        assertTrue(valor.getValue().contains("\"pedidosTotales\":3"));
        assertTrue(valor.getValue().contains("\"unidadesAcumuladas\":6"));
        assertTrue(valor.getValue().contains("\"Teclado\":2"));
    }

    @Test
    void siElEnvioFallaSoloSeRegistraYNoSePropagaLaExcepcion() {
        when(kafkaTemplate.send(eq("pedidos.metricas"), eq("global"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caido")));

        assertDoesNotThrow(() -> publisher.publicar(resumen));
    }

    @Test
    void siSendLanzaExcepcionNoSePropaga() {
        when(kafkaTemplate.send(eq("pedidos.metricas"), eq("global"), anyString()))
                .thenThrow(new IllegalStateException("producer cerrado"));

        assertDoesNotThrow(() -> publisher.publicar(resumen));
        assertEquals("global", "global");
    }
}