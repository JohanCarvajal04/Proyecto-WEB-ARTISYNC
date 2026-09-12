package uteq.edu.ec.artisync.repository.comunicacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.PortfolioComment;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PortfolioComment}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface PortfolioCommentRepository extends JpaRepository<PortfolioComment, Long> {

    /** Comentarios de una obra del portafolio en un estado de moderación dado, paginados. */
    Page<PortfolioComment> findByItemPortafolioIdItemPortafolioAndEstadoModeracion(
            Long idItem, String estadoModeracion, Pageable pageable);

    /**
     * Igual que findById, pero con bloqueo pesimista de fila. ocultarComentario
     * y reactivarComentario no tenían ningún lock: dos moderadores actuando
     * casi a la vez sobre el mismo comentario podían pisarse la decisión sin
     * ningún aviso (gana el último save/flush). Serializa esas dos llamadas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PortfolioComment c WHERE c.idComentario = :idComentario")
    Optional<PortfolioComment> findByIdParaModerar(@Param("idComentario") Long idComentario);

    /**
     * Conteo público (badge de la obra): solo cuenta los activos. Contar todos
     * sin filtrar inflaba el número con comentarios ocultos por moderación o
     * borrados lógicamente por su autor, que no aparecen en el listado público.
     */
    long countByItemPortafolioIdItemPortafolioAndEstadoModeracion(Long idItem, String estadoModeracion);
}

