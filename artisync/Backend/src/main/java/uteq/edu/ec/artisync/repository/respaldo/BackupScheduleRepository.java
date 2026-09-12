package uteq.edu.ec.artisync.repository.respaldo;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.artisync.entity.respaldo.BackupSchedule;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link BackupSchedule}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {

    /** Programaciones activas cuya próxima ejecución ya venció, usado por {@code BackupScheduler}. */
    List<BackupSchedule> findByActivoTrueAndProximaEjecucionLessThanEqual(LocalDateTime ahora);
}


