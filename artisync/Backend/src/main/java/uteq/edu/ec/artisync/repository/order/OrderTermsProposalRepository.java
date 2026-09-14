package uteq.edu.ec.artisync.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.artisync.entity.order.OrderTermsProposal;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link OrderTermsProposal}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface OrderTermsProposalRepository extends JpaRepository<OrderTermsProposal, Long> {

    /** Propuesta de términos de un pedido en un estado dado (p. ej. la pendiente de respuesta), si existe. */
    Optional<OrderTermsProposal> findByPedidoIdPedidoAndEstado(Long idPedido, String estado);
}


