package cl.duoc.analitica.kafka;

import cl.duoc.analitica.config.KafkaProperties;
import cl.duoc.analitica.model.MetricasResumen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Publica el resumen de metricas en pedidos.metricas (clave fija "global"). Los fallos solo se registran en el log. */
@Component
public class MetricasPublisher {

    private static final Logger log = LoggerFactory.getLogger(MetricasPublisher.class);
    private static final String CLAVE = "global";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final KafkaProperties props;

    public MetricasPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper, KafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.props = props;
    }

    public void publicar(MetricasResumen resumen) {
        try {
            String json = jsonMapper.writeValueAsString(resumen);
            kafkaTemplate.send(props.topics().metricas(), CLAVE, json).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("Kafka: no se pudo publicar metricas en {}: {}", props.topics().metricas(), ex.toString());
                } else {
                    log.info("Kafka: metricas publicadas en {} particion={} offset={}",
                            props.topics().metricas(), res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Kafka: error al publicar metricas: {}", e.toString());
        }
    }
}