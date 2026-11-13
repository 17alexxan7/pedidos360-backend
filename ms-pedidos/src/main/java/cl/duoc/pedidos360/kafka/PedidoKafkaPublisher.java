package cl.duoc.pedidos360.kafka;

import cl.duoc.pedidos360.config.KafkaProperties;
import cl.duoc.pedidos360.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica los eventos de pedidos en Kafka (topico pedidos.eventos). El negocio
 * (PedidoService) solo llama a publicarPedidoCreado(); aqui no se crea ningun topico
 * (ya existen) y no se toca el flujo de RabbitMQ.
 *
 * La clave del mensaje es el id del pedido: todos los eventos de un mismo pedido
 * caen en la misma particion y conservan su orden.
 *
 * Si Kafka falla, el error se registra y el pedido se crea igual.
 */
@Component
public class PedidoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties props;
    private final JsonMapper jsonMapper;

    public PedidoKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                KafkaProperties props,
                                JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
        this.jsonMapper = jsonMapper;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        PedidoEventoKafka event = PedidoEventoKafka.desde(pedido, props.events().pedidoCreado());
        String topico = props.topics().eventos();
        String clave = String.valueOf(event.pedidoId());
        try {
            String json = jsonMapper.writeValueAsString(event);
            kafkaTemplate.send(topico, clave, json).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("Kafka: fallo al publicar eventId={} pedidoId={} en {}: {}",
                            event.eventId(), event.pedidoId(), topico, error.getMessage());
                } else {
                    log.info("Kafka: publicado eventId={} pedidoId={} en {} particion={} offset={}",
                            event.eventId(), event.pedidoId(), topico,
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Kafka: no se pudo publicar eventId={} pedidoId={}: {}",
                    event.eventId(), event.pedidoId(), e.getMessage());
        }
    }
}