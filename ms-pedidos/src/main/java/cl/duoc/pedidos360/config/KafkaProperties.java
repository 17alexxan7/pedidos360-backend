package cl.duoc.pedidos360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de topicos y tipos de evento de Kafka leidos de application.properties
 * (prefijo app.kafka). Es la unica fuente de nombres de Kafka: ningun otro codigo
 * debe escribir estos nombres como texto fijo.
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
        Topics topics,
        Events events) {

    /** Topicos del clúster (ya existen, se crearon en K1). */
    public record Topics(String eventos, String metricas) {
    }

    /** Valor del campo "tipo" de los eventos publicados. */
    public record Events(String pedidoCreado) {
    }
}