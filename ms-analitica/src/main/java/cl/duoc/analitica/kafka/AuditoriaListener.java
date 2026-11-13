package cl.duoc.analitica.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumer group 2: lee el mismo topico de forma independiente (solo deja traza en el log). */
@Component
public class AuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);

    @KafkaListener(topics = "${app.kafka.topics.eventos}", groupId = "${app.kafka.groups.auditoria}")
    public void auditar(ConsumerRecord<String, String> record) {
        log.info("Auditoria: clave={} particion={} offset={}", record.key(), record.partition(), record.offset());
    }
}