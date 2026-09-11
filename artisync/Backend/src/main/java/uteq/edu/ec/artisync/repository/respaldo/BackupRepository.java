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

    boolean existsByEstadoRespaldo(BackupStatus estado);

    List<Backup> findByEstadoRespaldo(BackupStatus estado);

    Optional<Backup> findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(Long idRespaldoFullBase);

    boolean existsByIdRespaldoFullBaseAndEstadoRespaldo(Long idRespaldoFullBase, BackupStatus estado);

    Optional<Backup> findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(
            uteq.edu.ec.artisync.entity.respaldo.BackupType tipo, BackupStatus estado);
}


