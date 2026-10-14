package cl.duoc.pedidos360.repository;

import cl.duoc.pedidos360.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de pedidos (CRUD provisto por Spring Data JPA). */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}