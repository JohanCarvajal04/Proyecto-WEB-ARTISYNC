package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.InfraccionMensaje;

import java.time.LocalDateTime;

@Repository
public interface InfraccionRepository extends JpaRepository<InfraccionMensaje, Long> {

    long countByUsuarioIdUsuarioAndFechaInfraccionAfter(Long idUsuario, LocalDateTime fecha);

    Page<InfraccionMensaje> findByUsuarioIdUsuario(Long idUsuario, Pageable pageable);

    /** REQ-F-015 - fn_registrar_infraccion: inserta la infraccion, cuenta el total en 30 dias y suspende la cuenta al llegar a 3. Devuelve JSONB serializado como texto. */
    @Query(value = "SELECT fn_registrar_infraccion(:p_id_usuario, :p_id_pedido, :p_mensaje_original, :p_patron_detectado)::text", nativeQuery = true)
    String registrarInfraccion(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_id_pedido") Long idPedido,
            @Param("p_mensaje_original") String mensajeOriginal,
            @Param("p_patron_detectado") String patronDetectado);
}
