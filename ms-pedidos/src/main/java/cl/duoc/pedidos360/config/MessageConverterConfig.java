package cl.duoc.pedidos360.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conversor JSON para RabbitMQ. Spring Boot lo aplica solo al RabbitTemplate,
 * asi los eventos viajan como JSON y no como objetos serializados de Java.
 */
@Configuration
public class MessageConverterConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}