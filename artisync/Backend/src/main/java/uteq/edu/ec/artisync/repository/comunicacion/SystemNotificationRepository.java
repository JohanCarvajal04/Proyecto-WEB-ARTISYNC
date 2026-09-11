package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.SystemNotification;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link SystemNotification}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {

    Page<SystemNotification> findByUsuarioIdUsuarioOrderByFechaEmisionDesc(Long idUsuario, Pageable pageable);

    long countByUsuarioIdUsuarioAndEstaLeidaFalse(Long idUsuario);

    @Modifying
    @Query("UPDATE SystemNotification n SET n.estaLeida = true WHERE n.usuario.idUsuario = :idUsuario AND n.estaLeida = false")
    int marcarTodasLeidas(Long idUsuario);
}


