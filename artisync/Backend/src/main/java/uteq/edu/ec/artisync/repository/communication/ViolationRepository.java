package uteq.edu.ec.artisync.repository.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.communication.MessageViolation;

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
public interface ViolationRepository extends JpaRepository<MessageViolation, Long>, ViolationRepositoryCustom {

    /**
     * Cantidad de infracciones de un usuario desde una fecha (usado para la ventana de 30 días de REQ-F-015).
     * @param idUsuario el identificador de usuario
     * @param fecha la fecha
     * @return el valor numerico calculado
     */
    long countByUsuarioIdUsuarioAndFechaInfraccionAfter(Long idUsuario, LocalDateTime fecha);

    /**
     * Infracciones de mensajería de un usuario, paginadas.
     * @param idUsuario el identificador de usuario
     * @param pageable la pageable
     * @return la pagina de MessageViolation encontrados
     */
    Page<MessageViolation> findByUsuarioIdUsuario(Long idUsuario, Pageable pageable);
}


