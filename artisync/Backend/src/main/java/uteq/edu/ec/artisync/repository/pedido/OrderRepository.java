package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.Order;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Order}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsuarioClienteIdUsuario(Long idUsuario);

    List<Order> findByServicioPerfilIdPerfil(Long idPerfil);

    List<Order> findByServicioPerfilUsuarioIdUsuario(Long idUsuario);
}


