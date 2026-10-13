package cl.duoc.documentos.messaging;

/**
 * Evento de pedido creado, tal como lo publica ms-pedidos (JSON).
 * Misma estructura que PedidoCreadoEvent del productor.
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
}