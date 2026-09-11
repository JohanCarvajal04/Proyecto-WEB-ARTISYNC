package uteq.edu.ec.artisync.repository.respaldo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Respaldo}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface RespaldoRepository extends JpaRepository<Respaldo, Long>, JpaSpecificationExecutor<Respaldo> {

    boolean existsByEstadoRespaldo(EstadoRespaldo estado);

    List<Respaldo> findByEstadoRespaldo(EstadoRespaldo estado);

    Optional<Respaldo> findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(Long idRespaldoFullBase);

    boolean existsByIdRespaldoFullBaseAndEstadoRespaldo(Long idRespaldoFullBase, EstadoRespaldo estado);

    Optional<Respaldo> findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(
            uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo tipo, EstadoRespaldo estado);
}


