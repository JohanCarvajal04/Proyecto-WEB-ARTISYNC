package uteq.edu.ec.artisync.repository.respaldo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uteq.edu.ec.artisync.entity.respaldo.BackupStatus;
import uteq.edu.ec.artisync.entity.respaldo.Backup;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Backup}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface BackupRepository extends JpaRepository<Backup, Long>, JpaSpecificationExecutor<Backup> {

    /** @return {@code true} si hay algún respaldo en ese estado (usado para bloquear solicitudes concurrentes) */
    boolean existsByEstadoRespaldo(BackupStatus estado);

    /** Respaldos en un estado dado (p. ej. {@code COMPLETADO}, para el barrido de retención). */
    List<Backup> findByEstadoRespaldo(BackupStatus estado);

    /** El incremental más reciente que depende de un respaldo FULL, si hay alguno. */
    Optional<Backup> findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(Long idRespaldoFullBase);

    /** @return {@code true} si existe un incremental en ese estado que dependa de ese FULL */
    boolean existsByIdRespaldoFullBaseAndEstadoRespaldo(Long idRespaldoFullBase, BackupStatus estado);

    /** El respaldo más reciente de un tipo y estado dados. */
    Optional<Backup> findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(
            uteq.edu.ec.artisync.entity.respaldo.BackupType tipo, BackupStatus estado);
}


