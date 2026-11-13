package cl.duoc.analitica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Unica fuente de nombres de topicos, grupos y tipos de evento (application.yml, prefijo app.kafka). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(Topics topics, Groups groups, Events events, Retry retry) {

    public record Topics(String eventos, String metricas, String eventosDlt) {}

    public record Groups(String analitica, String auditoria) {}

    public record Events(String pedidoCreado) {}

    public record Retry(long intervaloMs, long maxReintentos) {}
}