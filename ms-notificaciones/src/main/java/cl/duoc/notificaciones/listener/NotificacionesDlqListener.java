package cl.duoc.notificaciones.listener;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consumidor de la DLQ de notificaciones (cola.notificaciones.dlq).
 * Solo registra el mensaje muerto (cuerpo y cabecera x-death con el motivo) y lo
 * confirma con ACK. Recibe el Message crudo para no fallar si el cuerpo no es JSON valido.
 * Se puede apagar con app.messaging.notificaciones.dlq-listener-enabled=false para
 * dejar los mensajes visibles en el panel de RabbitMQ.
 */
@Component
public class NotificacionesDlqListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesDlqListener.class);

    @RabbitListener(
            queues = "${app.messaging.notificaciones.dlq}", containerFactory = "dlqContainerFactory",
            autoStartup = "${app.messaging.notificaciones.dlq-listener-enabled:true}")
    public void onMensajeMuerto(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        String cuerpo = new String(message.getBody(), StandardCharsets.UTF_8);
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        log.error("MENSAJE MUERTO en DLQ de notificaciones. x-death={} cuerpo={}", xDeath, cuerpo);
        channel.basicAck(tag, false);
    }
}