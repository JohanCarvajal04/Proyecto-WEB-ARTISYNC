package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.OfferingReview;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link OfferingReview}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface OfferingReviewRepository extends JpaRepository<OfferingReview, Long> {

    /** Verifica si un pedido ya tiene reseña (constraint UNIQUE en BD). */
    boolean existsByPedidoIdPedido(Long idPedido);

    /** Obtiene la reseña de un pedido (relación 1:1), si existe. */
    Optional<OfferingReview> findByPedidoIdPedido(Long idPedido);

    /**
     * Lista las reseñas de todos los pedidos de un creador específico.
     * Se navega: resena → pedido → servicio → perfilCreador.
     */
    @Query("SELECT r FROM OfferingReview r " +
           "JOIN r.pedido p " +
           "JOIN p.servicio s " +
           "WHERE s.perfil.idPerfil = :idPerfil " +
           "ORDER BY r.fechaResena DESC")
    List<OfferingReview> findByCreadorIdPerfil(@Param("idPerfil") Long idPerfil);

    /**
     * Calcula el promedio de calificaciones de un creador.
     */
    @Query("SELECT AVG(r.calificacionEstrellas) FROM OfferingReview r " +
           "JOIN r.pedido p " +
           "JOIN p.servicio s " +
           "WHERE s.perfil.idPerfil = :idPerfil")
    Double calcularPromedioByCreadorIdPerfil(@Param("idPerfil") Long idPerfil);
}

