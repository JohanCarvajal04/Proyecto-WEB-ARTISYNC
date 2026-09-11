package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.ResenaServicio;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link ResenaServicio}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface ResenaServicioRepository extends JpaRepository<ResenaServicio, Long> {

    /** Verifica si un pedido ya tiene reseÃ±a (constraint UNIQUE en BD). */
    boolean existsByPedidoIdPedido(Long idPedido);

    /** Obtiene la reseÃ±a de un pedido (relaciÃ³n 1:1), si existe. */
    Optional<ResenaServicio> findByPedidoIdPedido(Long idPedido);

    /**
     * Lista las reseÃ±as de todos los pedidos de un creador especÃ­fico.
     * Se navega: resena â†’ pedido â†’ servicio â†’ perfilCreador.
     */
    @Query("SELECT r FROM ResenaServicio r " +
           "JOIN r.pedido p " +
           "JOIN p.servicio s " +
           "WHERE s.perfil.idPerfil = :idPerfil " +
           "ORDER BY r.fechaResena DESC")
    List<ResenaServicio> findByCreadorIdPerfil(@Param("idPerfil") Long idPerfil);

    /**
     * Calcula el promedio de calificaciones de un creador.
     */
    @Query("SELECT AVG(r.calificacionEstrellas) FROM ResenaServicio r " +
           "JOIN r.pedido p " +
           "JOIN p.servicio s " +
           "WHERE s.perfil.idPerfil = :idPerfil")
    Double calcularPromedioByCreadorIdPerfil(@Param("idPerfil") Long idPerfil);
}

