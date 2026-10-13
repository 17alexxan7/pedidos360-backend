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
 * Topologia del dominio DOCUMENTOS.
 *
 * Ruta normal:
 *   pedido creado -> pedidos.direct -> pedido.documento -> cola.documentos
 * Ruta de error (mensaje rechazado con NACK sin reencolar):
 *   cola.documentos -> pedidos.dlx -> documentos.dead -> cola.documentos.dlq
 *
 * Todos los nombres vienen de app.messaging.documentos.* (MessagingProperties).
 * Si otro servicio declara esta misma cola, debe usar argumentos identicos
 * (si no, RabbitMQ responde PRECONDITION_FAILED).
 */
@Configuration
public class DocumentosRabbitConfig {

    private final MessagingProperties props;

    public DocumentosRabbitConfig(MessagingProperties props) {
        this.props = props;
    }

    /** Cola principal: duradera, con DLX y routing key de la DLQ. */
    @Bean
    public Queue documentosQueue() {
        return QueueBuilder.durable(props.documentos().queue())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.documentos().dlqRoutingKey())
                .build();
    }

    /** Dead-letter queue: guarda los mensajes que no se pudieron procesar. */
    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(props.documentos().dlq()).build();
    }

    /** Binding cola principal -> exchange direct con la routing key de documentos. */
    @Bean
    public Binding documentosBinding(@Qualifier("pedidosDirectExchange") DirectExchange direct) {
        return BindingBuilder.bind(documentosQueue())
                .to(direct)
                .with(props.documentos().routingKey());
    }

    /** Binding DLQ -> DLX con la routing key de mensajes muertos. */
    @Bean
    public Binding documentosDlqBinding(@Qualifier("pedidosDlx") DirectExchange dlx) {
        return BindingBuilder.bind(documentosDlq())
                .to(dlx)
                .with(props.documentos().dlqRoutingKey());
    }
}