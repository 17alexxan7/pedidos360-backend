package cl.duoc.pedidos360.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara los exchanges de Pedidos360. Los nombres vienen de app.messaging.exchanges.*
 * - pedidos.direct: ruteo exacto (tickets, documentos)
 * - pedidos.topic: ruteo por patron (notificaciones)
 * - pedidos.dlx: dead-letter exchange, recibe los mensajes rechazados de las colas principales
 */
@Configuration
public class ExchangesConfig {

    private final MessagingProperties props;

    public ExchangesConfig(MessagingProperties props) {
        this.props = props;
    }

    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(props.exchanges().direct(), true, false);
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(props.exchanges().topic(), true, false);
    }

    @Bean
    public DirectExchange pedidosDlx() {
        return new DirectExchange(props.exchanges().dlx(), true, false);
    }
}