package cl.duoc.pedidos360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys leidos de application.properties
 * (prefijo app.messaging). Es la unica fuente de nombres de mensajeria:
 * ningun otro codigo debe escribir estos nombres como texto fijo.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        Exchanges exchanges,
        Events events,
        Route notificaciones,
        Route tickets,
        Route documentos) {

    /** Exchanges: direct, topic y dead-letter (DLX). */
    public record Exchanges(String direct, String topic, String dlx) {
    }

    /** Routing keys con las que se publican los eventos. */
    public record Events(String pedidoCreado) {
    }

    /** Ruta de un caso de uso: cola principal, su DLQ y las routing keys. */
    public record Route(String queue, String dlq, String routingKey, String dlqRoutingKey) {
    }
}