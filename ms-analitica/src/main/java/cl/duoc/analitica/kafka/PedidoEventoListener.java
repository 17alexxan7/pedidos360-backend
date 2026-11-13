package cl.duoc.analitica.kafka;

import cl.duoc.analitica.config.KafkaProperties;
import cl.duoc.analitica.exception.EventoInvalidoException;
import cl.duoc.analitica.model.PedidoEvento;
import cl.duoc.analitica.service.MetricasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Consumer group 1: calcula metricas. Si el evento es invalido lanza EventoInvalidoException (va al DLT sin reintentos). */
@Component
public class PedidoEventoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoListener.class);

    private final JsonMapper jsonMapper;
    private final KafkaProperties props;
    private final MetricasService metricas;
    private final MetricasPublisher publisher;

    public PedidoEventoListener(JsonMapper jsonMapper, KafkaProperties props,
                                MetricasService metricas, MetricasPublisher publisher) {
        this.jsonMapper = jsonMapper;
        this.props = props;
        this.metricas = metricas;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${app.kafka.topics.eventos}", groupId = "${app.kafka.groups.analitica}")
    public void consumir(ConsumerRecord<String, String> record) {
        PedidoEvento evento = parsear(record.value());
        validar(evento);

        if (metricas.registrar(evento)) {
            log.info("Analitica: evento {} procesado pedidoId={} (particion={} offset={})",
                    evento.eventId(), evento.pedidoId(), record.partition(), record.offset());
            publisher.publicar(metricas.resumen());
        } else {
            log.warn("Analitica: evento {} repetido, se ignora", evento.eventId());
        }
    }

    private PedidoEvento parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new EventoInvalidoException("Mensaje vacio");
        }
        try {
            return jsonMapper.readValue(valor, PedidoEvento.class);
        } catch (JacksonException e) {
            throw new EventoInvalidoException("JSON invalido: " + e.getClass().getSimpleName(), e);
        }
    }

    private void validar(PedidoEvento e) {
        if (!props.events().pedidoCreado().equals(e.tipo())) {
            throw new EventoInvalidoException("Tipo de evento desconocido: " + e.tipo());
        }
        if (e.eventId() == null || e.pedidoId() == null
                || e.producto() == null || e.producto().isBlank()
                || e.estado() == null || e.estado().isBlank()
                || e.cantidad() <= 0) {
            throw new EventoInvalidoException("Evento con campos obligatorios faltantes o invalidos");
        }
    }
}