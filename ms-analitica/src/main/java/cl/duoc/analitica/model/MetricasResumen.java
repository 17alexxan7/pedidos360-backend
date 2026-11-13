package cl.duoc.analitica.model;

import java.time.Instant;
import java.util.Map;

public record MetricasResumen(
        long pedidosTotales,
        long unidadesAcumuladas,
        Map<String, Long> pedidosPorEstado,
        Map<String, Long> pedidosPorProducto,
        Instant actualizadoEn) {
}