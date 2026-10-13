package cl.duoc.rabbitadmin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.rabbitadmin.dto.BindingRequest;
import cl.duoc.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.rabbitadmin.dto.ExchangeType;
import cl.duoc.rabbitadmin.dto.QueueRequest;
import cl.duoc.rabbitadmin.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        service = new RabbitAdminService(amqpAdmin);
    }

    @Test
    void createQueueDeclaraLaColaConElNombreIndicado() {
        service.createQueue(new QueueRequest("cola.test", true, null, null, null));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertEquals("cola.test", captor.getValue().getName());
    }

    @Test
    void createQueueRechazaRoutingKeyDeDlqSinExchange() {
        QueueRequest req = new QueueRequest("cola.test", true, null, "dead", null);

        assertThrows(IllegalArgumentException.class, () -> service.createQueue(req));
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void createExchangeTopicDeclaraUnTopicExchange() {
        service.createExchange(new ExchangeRequest("ex.test", ExchangeType.TOPIC, true));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertInstanceOf(TopicExchange.class, captor.getValue());
        assertEquals("ex.test", captor.getValue().getName());
    }

    @Test
    void getQueueInexistenteLanzaResourceNotFound() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.getQueue("no.existe"));
    }

    @Test
    void createBindingConColaInexistenteLanzaResourceNotFound() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);
        BindingRequest req = new BindingRequest("no.existe", "ex.test", "clave");

        assertThrows(ResourceNotFoundException.class, () -> service.createBinding(req));
    }
}