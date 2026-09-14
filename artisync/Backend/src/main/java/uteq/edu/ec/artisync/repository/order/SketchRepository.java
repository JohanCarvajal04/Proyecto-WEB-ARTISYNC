package uteq.edu.ec.artisync.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.order.Sketch;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Sketch}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 */
@Repository
public interface SketchRepository extends JpaRepository<Sketch, Long> {

    /** El boceto vigente de un pedido (relación 1:1), si existe. */
    Optional<Sketch> findByPedidoIdPedido(Long idPedido);

    /** @return {@code true} si el pedido ya tiene un boceto subido */
    boolean existsByPedidoIdPedido(Long idPedido);
}
