package cl.duoc.pedidos360.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

/**
 * Callbacks del RabbitTemplate:
 * - confirm: RabbitMQ confirma (ack) o rechaza (nack) cada mensaje publicado.
 * - return: avisa cuando un mensaje no llego a ninguna cola (sin binding).
 */
@Configuration
public class PublisherCallbacksConfig {

    private static final Logger log = LoggerFactory.getLogger(PublisherCallbacksConfig.class);

    public PublisherCallbacksConfig(RabbitTemplate rabbitTemplate) {
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            String id = correlation != null ? correlation.getId() : "sin-id";
            if (ack) {
                log.debug("Confirmado por RabbitMQ: {}", id);
            } else {
                log.error("RabbitMQ rechazo el mensaje {}: {}", id, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "Mensaje sin ruta: exchange={} routingKey={} motivo={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
    }
}