package cl.duoc.pedidos360.service;

import cl.duoc.pedidos360.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final PedidoEventPublisher publisher;

    public PedidoService(PedidoRepository repository, PedidoEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /*
     * Datos de ejemplo para poder probar los endpoints apenas se levanta el backend.
     * Solo se insertan si la tabla esta vacia y no publican eventos (usan guardar(), no crear()).
     */
    @PostConstruct
    void sembrarDatosDeEjemplo() {
        if (repository.count() == 0) {
            guardar(new Pedido(null, "Ana Pérez", "Notebook", 1, "PENDIENTE"));
            guardar(new Pedido(null, "Juan Soto", "Mouse inalámbrico", 2, "ENVIADO"));
        }
    }

    public List<Pedido> listarTodos() {
        return repository.findAll();
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Pedido crear(Pedido pedido) {
        Pedido creado = guardar(pedido);
        publisher.publicarPedidoCreado(creado);
        return creado;
    }

    private Pedido guardar(Pedido pedido) {
        pedido.setId(null);

        if (pedido.getEstado() == null || pedido.getEstado().isBlank()) {
            pedido.setEstado("PENDIENTE");
        }

        return repository.save(pedido);
    }

    public Optional<Pedido> actualizar(Long id, Pedido datosActualizados) {
        return repository.findById(id).map(existente -> {
            existente.setCliente(datosActualizados.getCliente());
            existente.setProducto(datosActualizados.getProducto());
            existente.setCantidad(datosActualizados.getCantidad());
            existente.setEstado(datosActualizados.getEstado());
            return repository.save(existente);
        });
    }

    public boolean eliminar(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}