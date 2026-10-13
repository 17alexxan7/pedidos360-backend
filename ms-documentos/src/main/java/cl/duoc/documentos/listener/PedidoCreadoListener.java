package cl.duoc.documentos.listener;

import cl.duoc.documentos.exception.MensajeInvalidoException;
import cl.duoc.documentos.messaging.PedidoCreadoEvent;
import cl.duoc.documentos.service.DocumentoService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del dominio documentos (cola.documentos), con ACK manual.
 *
 * Politica de errores:
 * - Exito: basicAck.
 * - Error irrecuperable (MensajeInvalidoException): basicNack sin reencolar -> DLQ, sin reintentos.
 * - Error recuperable (otra RuntimeException): hasta max-attempts intentos con espera
 *   backoff-ms entre ellos; si se agotan, basicNack sin reencolar -> DLQ.
 * Cada fallo se registra en el log con el eventId del mensaje.
 */
@Component
public class PedidoCreadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoCreadoListener.class);

    private final DocumentoService service;
    private final int maxIntentos;
    private final long esperaMs;

    public PedidoCreadoListener(DocumentoService service,
                                @Value("${app.messaging.retry.max-attempts:3}") int maxIntentos,
                                @Value("${app.messaging.retry.backoff-ms:2000}") long esperaMs) {
        this.service = service;
        this.maxIntentos = maxIntentos;
        this.esperaMs = esperaMs;
    }

    @RabbitListener(queues = "${app.messaging.documentos.queue}")
    public void onPedidoCreado(PedidoCreadoEvent evento, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                service.enviar(evento);
                channel.basicAck(tag, false);
                log.info("ACK eventId={} pedidoId={} (intento {})", evento.eventId(), evento.pedidoId(), intento);
                return;
            } catch (MensajeInvalidoException e) {
                log.error("NACK -> DLQ (irrecuperable) eventId={} motivo={}", evento.eventId(), e.getMessage());
                channel.basicNack(tag, false, false);
                return;
            } catch (RuntimeException e) {
                log.warn("Fallo recuperable eventId={} intento {}/{} motivo={}",
                        evento.eventId(), intento, maxIntentos, e.getMessage());
                if (intento < maxIntentos && !esperar()) {
                    channel.basicNack(tag, false, true);
                    return;
                }
            }
        }
        log.error("NACK -> DLQ (reintentos agotados) eventId={} pedidoId={}", evento.eventId(), evento.pedidoId());
        channel.basicNack(tag, false, false);
    }

    /** Espera entre reintentos; devuelve false si el hilo fue interrumpido (cierre del servicio). */
    private boolean esperar() {
        try {
            Thread.sleep(esperaMs);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}