package cl.duoc.documentos.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conversor JSON para RabbitMQ. Spring Boot lo aplica solo a los listeners,
 * asi el cuerpo del mensaje se convierte al DTO del parametro del metodo.
 */
@Configuration
public class MessageConverterConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}