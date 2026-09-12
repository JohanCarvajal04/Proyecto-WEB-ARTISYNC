package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.Sketch;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Sketch}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 */
@Repository
public interface SketchRepository extends JpaRepository<Sketch, Long> {

    Optional<Sketch> findByPedidoIdPedido(Long idPedido);

    boolean existsByPedidoIdPedido(Long idPedido);
}
