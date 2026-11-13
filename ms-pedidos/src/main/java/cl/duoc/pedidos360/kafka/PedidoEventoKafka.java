package cl.duoc.pedidos360.kafka;

import cl.duoc.pedidos360.model.Pedido;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento que se publica en Kafka (topico pedidos.eventos) cuando se crea un pedido.
 * Viaja como JSON. "tipo" permite que otros consumidores distingan futuros eventos.
 */
public record PedidoEventoKafka(
        String eventId,
        String tipo,
        Long pedidoId,
        String cliente,
        String producto,
        int cantidad,
        String estado,
        String occurredAt) {

    public static PedidoEventoKafka desde(Pedido pedido, String tipo) {
        return new PedidoEventoKafka(
                UUID.randomUUID().toString(),
                tipo,
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProducto(),
                pedido.getCantidad(),
                pedido.getEstado(),
                Instant.now().toString());
    }
}