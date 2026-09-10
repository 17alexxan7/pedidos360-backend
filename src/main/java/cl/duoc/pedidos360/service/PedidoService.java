package cl.duoc.pedidos360.service;

import cl.duoc.pedidos360.model.Pedido;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PedidoService {

    private final Map<Long, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong contadorId = new AtomicLong(1);

    public PedidoService() {
        /*
         * Datos de ejemplo para poder probar los endpoints
         * apenas se levanta el backend.
         */
        crear(new Pedido(null, "Ana Pérez", "Notebook", 1, "PENDIENTE"));
        crear(new Pedido(null, "Juan Soto", "Mouse inalámbrico", 2, "ENVIADO"));
    }

    public List<Pedido> listarTodos() {
        return List.copyOf(pedidos.values());
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return Optional.ofNullable(pedidos.get(id));
    }

    public Pedido crear(Pedido pedido) {
        Long nuevoId = contadorId.getAndIncrement();
        pedido.setId(nuevoId);

        if (pedido.getEstado() == null || pedido.getEstado().isBlank()) {
            pedido.setEstado("PENDIENTE");
        }

        pedidos.put(nuevoId, pedido);
        return pedido;
    }

    public Optional<Pedido> actualizar(Long id, Pedido datosActualizados) {
        Pedido existente = pedidos.get(id);

        if (existente == null) {
            return Optional.empty();
        }

        existente.setCliente(datosActualizados.getCliente());
        existente.setProducto(datosActualizados.getProducto());
        existente.setCantidad(datosActualizados.getCantidad());
        existente.setEstado(datosActualizados.getEstado());

        return Optional.of(existente);
    }

    public boolean eliminar(Long id) {
        return pedidos.remove(id) != null;
    }
}