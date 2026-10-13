package cl.duoc.pedidos360.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia del dominio NOTIFICACIONES.
 *
 * Ruta normal:
 *   pedido creado -> pedidos.topic -> pedido.creado.# -> cola.notificaciones
 * Ruta de error (mensaje rechazado con NACK sin reencolar):
 *   cola.notificaciones -> pedidos.dlx -> notificaciones.dead -> cola.notificaciones.dlq
 *
 * Todos los nombres vienen de app.messaging.notificaciones.* (MessagingProperties).
 * Si otro servicio declara esta misma cola, debe usar argumentos identicos
 * (si no, RabbitMQ responde PRECONDITION_FAILED).
 */
@Configuration
public class NotificacionesRabbitConfig {

    private final MessagingProperties props;

    public NotificacionesRabbitConfig(MessagingProperties props) {
        this.props = props;
    }

    /** Cola principal: duradera, con DLX y routing key de la DLQ. */
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(props.notificaciones().queue())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.notificaciones().dlqRoutingKey())
                .build();
    }

    /** Dead-letter queue: guarda los mensajes que no se pudieron procesar. */
    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.notificaciones().dlq()).build();
    }

    /** Binding cola principal -> exchange topic con el patron de pedido creado. */
    @Bean
    public Binding notificacionesBinding(TopicExchange pedidosTopicExchange) {
        return BindingBuilder.bind(notificacionesQueue())
                .to(pedidosTopicExchange)
                .with(props.notificaciones().routingKey());
    }

    /** Binding DLQ -> DLX con la routing key de mensajes muertos. */
    @Bean
    public Binding notificacionesDlqBinding(@Qualifier("pedidosDlx") DirectExchange dlx) {
        return BindingBuilder.bind(notificacionesDlq())
                .to(dlx)
                .with(props.notificaciones().dlqRoutingKey());
    }
}