package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.artisync.entity.pedido.PropuestaTerminosPedido;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PropuestaTerminosPedido}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface PropuestaTerminosPedidoRepository extends JpaRepository<PropuestaTerminosPedido, Long> {

    Optional<PropuestaTerminosPedido> findByPedidoIdPedidoAndEstado(Long idPedido, String estado);
}


