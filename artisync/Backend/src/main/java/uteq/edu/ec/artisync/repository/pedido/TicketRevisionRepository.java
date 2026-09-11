package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link TicketRevision}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface TicketRevisionRepository extends JpaRepository<TicketRevision, Long> {

    List<TicketRevision> findByPedidoIdPedidoOrderByIdTicketDesc(Long idPedido);

    long countByPedidoIdPedido(Long idPedido);
}


