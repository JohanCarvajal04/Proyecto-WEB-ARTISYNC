package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.SentBriefing;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link SentBriefing}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface SentBriefingRepository extends JpaRepository<SentBriefing, Long> {

    /** El briefing enviado para un pedido (relación 1:1), si existe. */
    Optional<SentBriefing> findByPedidoIdPedido(Long idPedido);

    /** @return {@code true} si el pedido ya tiene un briefing enviado */
    boolean existsByPedidoIdPedido(Long idPedido);
}


