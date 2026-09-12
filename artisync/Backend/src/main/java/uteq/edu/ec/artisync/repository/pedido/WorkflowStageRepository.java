package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStage;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link WorkflowStage}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {

    /** Etapa maestra por nombre exacto. */
    Optional<WorkflowStage> findByNombreEtapa(String nombreEtapa);

    /** @return {@code true} si ya existe una etapa maestra con ese nombre */
    boolean existsByNombreEtapa(String nombreEtapa);
}


