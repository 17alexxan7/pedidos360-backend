package cl.duoc.pedidos360.messaging;

import cl.duoc.pedidos360.model.Pedido;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento que se publica cuando se crea un pedido. Viaja como JSON por RabbitMQ.
 * eventId identifica el mensaje en los logs de productor y consumidores.
 */
public record PedidoCreadoEvent(
        String eventId,
        Long pedidoId,
        String cliente,
        String producto,
        int cantidad,
        String estado,
        String occurredAt) {

    public static PedidoCreadoEvent desde(Pedido pedido) {
        return new PedidoCreadoEvent(
                UUID.randomUUID().toString(),
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProducto(),
                pedido.getCantidad(),
                pedido.getEstado(),
                Instant.now().toString());
    }
}