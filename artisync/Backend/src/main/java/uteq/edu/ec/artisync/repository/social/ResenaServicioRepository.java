package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.ResenaServicio;

import java.util.List;

/**
 * Repositorio de reseñas de servicios.
 * RF-09: Calificaciones 1-5 estrellas, una por pedido, solo post-entrega.
 */
@Repository
public interface ResenaServicioRepository extends JpaRepository<ResenaServicio, Long> {

    /** Verifica si un pedido ya tiene reseña (constraint UNIQUE en BD). */
    boolean existsByPedidoIdPedido(Long idPedido);

    /**
     * Lista las reseñas de todos los pedidos de un creador específico.
     * Se navega: resena → pedido → servicio → perfilCreador.
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
