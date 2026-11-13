package cl.duoc.analitica.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contrato del evento publicado por ms-pedidos en pedidos.eventos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEvento(
        String eventId,
        String tipo,
        Long pedidoId,
        String cliente,
        String producto,
        int cantidad,
        String estado,
        String occurredAt) {
}