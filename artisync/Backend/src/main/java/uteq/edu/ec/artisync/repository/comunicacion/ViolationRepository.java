package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.MessageViolation;

import java.time.LocalDateTime;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link MessageViolation}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface ViolationRepository extends JpaRepository<MessageViolation, Long> {

    /** Cantidad de infracciones de un usuario desde una fecha (usado para la ventana de 30 días de REQ-F-015). */
    long countByUsuarioIdUsuarioAndFechaInfraccionAfter(Long idUsuario, LocalDateTime fecha);

    /** Infracciones de mensajería de un usuario, paginadas. */
    Page<MessageViolation> findByUsuarioIdUsuario(Long idUsuario, Pageable pageable);

    /**
     * REQ-F-015 - fn_registrar_infraccion: inserta la infraccion, cuenta el total en 30 dias y
     * suspende la cuenta al llegar a 3. Devuelve JSONB serializado como texto.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * {@code @Procedure} con retorno no-void rompe con Hibernate 7.4.1 contra una FUNCTION de Postgres
     * (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC, invalida). Ver el
     * hallazgo completo en docs/basedatos/CATALOGO-SP.md §14.
     */
    @Query(value = "SELECT fn_registrar_infraccion(:p_id_usuario, :p_id_pedido, :p_mensaje_original, :p_patron_detectado)::text", nativeQuery = true)
    String registerViolation(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_id_pedido") Long idPedido,
            @Param("p_mensaje_original") String mensajeOriginal,
            @Param("p_patron_detectado") String patronDetectado);
}


