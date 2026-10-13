package cl.duoc.pedidos360.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia del dominio TICKETS.
 *
 * Ruta normal:
 *   pedido creado -> pedidos.direct -> pedido.ticket -> cola.tickets
 * Ruta de error (mensaje rechazado con NACK sin reencolar):
 *   cola.tickets -> pedidos.dlx -> tickets.dead -> cola.tickets.dlq
 *
 * Todos los nombres vienen de app.messaging.tickets.* (MessagingProperties).
 * Si otro servicio declara esta misma cola, debe usar argumentos identicos
 * (si no, RabbitMQ responde PRECONDITION_FAILED).
 */
@Configuration
public class TicketsRabbitConfig {

    private final MessagingProperties props;

    public TicketsRabbitConfig(MessagingProperties props) {
        this.props = props;
    }

    /** Cola principal: duradera, con DLX y routing key de la DLQ. */
    @Bean
    public Queue ticketsQueue() {
        return QueueBuilder.durable(props.tickets().queue())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.tickets().dlqRoutingKey())
                .build();
    }

    /** Dead-letter queue: guarda los mensajes que no se pudieron procesar. */
    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(props.tickets().dlq()).build();
    }

    /** Binding cola principal -> exchange direct con la routing key de tickets. */
    @Bean
    public Binding ticketsBinding(@Qualifier("pedidosDirectExchange") DirectExchange direct) {
        return BindingBuilder.bind(ticketsQueue())
                .to(direct)
                .with(props.tickets().routingKey());
    }

    /** Binding DLQ -> DLX con la routing key de mensajes muertos. */
    @Bean
    public Binding ticketsDlqBinding(@Qualifier("pedidosDlx") DirectExchange dlx) {
        return BindingBuilder.bind(ticketsDlq())
                .to(dlx)
                .with(props.tickets().dlqRoutingKey());
    }
}