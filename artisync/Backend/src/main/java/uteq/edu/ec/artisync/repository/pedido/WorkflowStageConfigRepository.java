package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStageConfig;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link WorkflowStageConfig}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface WorkflowStageConfigRepository extends JpaRepository<WorkflowStageConfig, Long> {

    List<WorkflowStageConfig> findByFlujoIdFlujoOrderByNumeroOrdenAsc(Long idFlujo);

    List<WorkflowStageConfig> findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(Long idFlujo, Integer numeroOrden);

    boolean existsByFlujoIdFlujoAndEtapaIdEtapa(Long idFlujo, Long idEtapa);

    /** REQ-NF-018: Â¿la etapa actual de un pedido es la etapa final de su flujo? Usado para bloquear la supresiÃ³n de datos mientras el pedido sigue en curso. */
    boolean existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue(Long idFlujo, Long idEtapa);

    boolean existsByFlujoIdFlujoAndNumeroOrden(Long idFlujo, Integer numeroOrden);

    void deleteByFlujoIdFlujo(Long idFlujo);
}

