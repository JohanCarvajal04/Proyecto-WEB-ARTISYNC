package uteq.edu.ec.artisync.repository.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.communication.NotificationType;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link NotificationType}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {

    /**
     * Tipo de notificación por nombre de evento exacto (p. ej. "CUENTA_SUSPENDIDA").
     * @param nombreEvento el nombre de evento
     * @return un Optional con NotificationType si existe, vacio en caso contrario
     */
    Optional<NotificationType> findByNombreEvento(String nombreEvento);
}


