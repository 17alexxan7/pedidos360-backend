package cl.duoc.pedidos360.messaging;

import cl.duoc.pedidos360.config.MessagingProperties;
import cl.duoc.pedidos360.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de pedidos en RabbitMQ. El negocio (PedidoService) solo
 * llama a publicarPedidoCreado(); aqui no se declara ninguna cola ni exchange.
 *
 * Un pedido creado genera 3 mensajes persistentes:
 *   - pedidos.topic  + pedido.creado   -> notificaciones
 *   - pedidos.direct + pedido.ticket   -> tickets
 *   - pedidos.direct + pedido.documento -> documentos
 *
 * Si RabbitMQ falla, el error se registra y el pedido se crea igual.
 */
@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        PedidoCreadoEvent event = PedidoCreadoEvent.desde(pedido);
        try {
            enviar(props.exchanges().topic(), props.events().pedidoCreado(), event);
            enviar(props.exchanges().direct(), props.tickets().routingKey(), event);
            enviar(props.exchanges().direct(), props.documentos().routingKey(), event);
            log.info("Evento pedido creado publicado: eventId={} pedidoId={}", event.eventId(), event.pedidoId());
        } catch (Exception e) {
            log.error("No se pudo publicar el evento eventId={} pedidoId={}: {}",
                    event.eventId(), event.pedidoId(), e.getMessage());
        }
    }

    private void enviar(String exchange, String routingKey, PedidoCreadoEvent event) {
        CorrelationData correlation = new CorrelationData(event.eventId() + ":" + routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(event.eventId());
            return message;
        }, correlation);
    }
}