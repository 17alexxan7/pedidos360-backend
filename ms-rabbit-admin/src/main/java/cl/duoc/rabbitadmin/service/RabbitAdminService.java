package cl.duoc.rabbitadmin.service;

import cl.duoc.rabbitadmin.dto.BindingRequest;
import cl.duoc.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.rabbitadmin.dto.QueueRequest;
import cl.duoc.rabbitadmin.exception.RabbitOperationException;
import cl.duoc.rabbitadmin.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void createQueue(QueueRequest req) {
        boolean hasDlx = req.deadLetterExchange() != null && !req.deadLetterExchange().isBlank();
        boolean hasDlRoutingKey = req.deadLetterRoutingKey() != null && !req.deadLetterRoutingKey().isBlank();
        if (hasDlRoutingKey && !hasDlx) {
            throw new IllegalArgumentException("No se puede definir deadLetterRoutingKey sin deadLetterExchange");
        }

        boolean durable = req.durable() == null || req.durable();
        QueueBuilder builder = durable ? QueueBuilder.durable(req.name()) : QueueBuilder.nonDurable(req.name());
        if (hasDlx) {
            builder.deadLetterExchange(req.deadLetterExchange());
        }
        if (hasDlRoutingKey) {
            builder.deadLetterRoutingKey(req.deadLetterRoutingKey());
        }
        if (req.messageTtlMs() != null) {
            builder.withArgument("x-message-ttl", req.messageTtlMs());
        }

        try {
            amqpAdmin.declareQueue(builder.build());
            log.info("Cola creada: {} (durable={}, dlx={})", req.name(), durable, hasDlx ? req.deadLetterExchange() : "ninguno");
        } catch (AmqpException e) {
            throw new RabbitOperationException("No se pudo crear la cola '" + req.name() + "'. Si ya existe, debe tener la misma configuracion", e);
        }
    }

    public QueueInfoResponse getQueue(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void deleteQueue(String name) {
        getQueue(name);
        try {
            amqpAdmin.deleteQueue(name);
            log.info("Cola eliminada: {}", name);
        } catch (AmqpException e) {
            throw new RabbitOperationException("No se pudo eliminar la cola '" + name + "'", e);
        }
    }

    public void createExchange(ExchangeRequest req) {
        boolean durable = req.durable() == null || req.durable();
        Exchange exchange = switch (req.type()) {
            case DIRECT -> new DirectExchange(req.name(), durable, false);
            case TOPIC -> new TopicExchange(req.name(), durable, false);
            case FANOUT -> new FanoutExchange(req.name(), durable, false);
        };
        try {
            amqpAdmin.declareExchange(exchange);
            log.info("Exchange creado: {} (tipo={}, durable={})", req.name(), req.type(), durable);
        } catch (AmqpException e) {
            throw new RabbitOperationException("No se pudo crear el exchange '" + req.name() + "'. Si ya existe, debe tener el mismo tipo", e);
        }
    }

    public void deleteExchange(String name) {
        try {
            amqpAdmin.deleteExchange(name);
            log.info("Exchange eliminado: {}", name);
        } catch (AmqpException e) {
            throw new RabbitOperationException("No se pudo eliminar el exchange '" + name + "'", e);
        }
    }

    public void createBinding(BindingRequest req) {
        getQueue(req.queue());
        try {
            amqpAdmin.declareBinding(toBinding(req.queue(), req.exchange(), req.routingKey()));
            log.info("Binding creado: {} -> {} (routingKey='{}')", req.exchange(), req.queue(), req.routingKey());
        } catch (AmqpException e) {
            throw new RabbitOperationException("No se pudo crear el binding. Verifica que el exchange '" + req.exchange() + "' exista", e);
        }
    }

    public void deleteBinding(String queue, String exchange, String routingKey) {
        getQueue(queue);
        try {
            amqpAdmin.removeBinding(toBinding(queue, exchange, routingKey));
            log.info("Binding eliminado: {} -> {} (routingKey='{}')", exchange, queue, routingKey);
        } catch (AmqpException e) {
            throw new RabbitOperationException("No se pudo eliminar el binding", e);
        }
    }

    private Binding toBinding(String queue, String exchange, String routingKey) {
        String key = routingKey == null ? "" : routingKey;
        return new Binding(queue, Binding.DestinationType.QUEUE, exchange, key, null);
    }
}
