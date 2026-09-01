package uteq.edu.ec.artisync.repository.comunicacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.ComentarioPortafolio;

import java.util.Optional;

@Repository
public interface ComentarioPortafolioRepository extends JpaRepository<ComentarioPortafolio, Long> {

    Page<ComentarioPortafolio> findByItemPortafolioIdItemPortafolioAndEstadoModeracion(
            Long idItem, String estadoModeracion, Pageable pageable);

    /**
     * Igual que findById, pero con bloqueo pesimista de fila. ocultarComentario
     * y reactivarComentario no tenían ningún lock: dos moderadores actuando
     * casi a la vez sobre el mismo comentario podían pisarse la decisión sin
     * ningún aviso (gana el último save/flush). Serializa esas dos llamadas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ComentarioPortafolio c WHERE c.idComentario = :idComentario")
    Optional<ComentarioPortafolio> findByIdParaModerar(@Param("idComentario") Long idComentario);

    /**
     * Conteo público (badge de la obra): solo cuenta los activos. Contar todos
     * sin filtrar inflaba el número con comentarios ocultos por moderación o
     * borrados lógicamente por su autor, que no aparecen en el listado público.
     */
    long countByItemPortafolioIdItemPortafolioAndEstadoModeracion(Long idItem, String estadoModeracion);
}
