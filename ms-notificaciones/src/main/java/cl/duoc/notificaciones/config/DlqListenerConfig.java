package cl.duoc.notificaciones.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contenedor propio para los listeners de DLQ. Usa SimpleMessageConverter (sin JSON)
 * porque un mensaje muerto conserva la cabecera __TypeId__ del productor, que el
 * convertidor JSON no puede cargar en este servicio. Mantiene ACK manual.
 */
@Configuration
public class DlqListenerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory dlqContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        factory.setMessageConverter(new SimpleMessageConverter());
        return factory;
    }
}