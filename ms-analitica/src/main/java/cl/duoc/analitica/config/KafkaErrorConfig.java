package cl.duoc.analitica.config;

import cl.duoc.analitica.exception.EventoInvalidoException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Manejo de errores: reintentos con espera y envio al DLT.
 * Spring Boot aplica este CommonErrorHandler a todos los @KafkaListener.
 */
@Configuration
public class KafkaErrorConfig {

    @Bean
    public NewTopic eventosDlt(KafkaProperties props) {
        return TopicBuilder.name(props.topics().eventosDlt()).partitions(3).replicas(3).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template, KafkaProperties props) {
        // Particion -1: Kafka elige la particion del DLT (no depende de la del original)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template, (record, ex) -> new TopicPartition(props.topics().eventosDlt(), -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(props.retry().intervaloMs(), props.retry().maxReintentos()));

        // JSON roto o tipo desconocido: no tiene sentido reintentar, va directo al DLT
        handler.addNotRetryableExceptions(EventoInvalidoException.class);
        return handler;
    }
}