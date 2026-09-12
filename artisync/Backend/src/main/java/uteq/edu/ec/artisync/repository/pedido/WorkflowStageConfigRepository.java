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

    /** Etapas configuradas de un flujo, en el orden en que se ejecutan. */
    List<WorkflowStageConfig> findByFlujoIdFlujoOrderByNumeroOrdenAsc(Long idFlujo);

    /** Etapas de un flujo posteriores a un número de orden dado, en orden. */
    List<WorkflowStageConfig> findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(Long idFlujo, Integer numeroOrden);

    /** @return {@code true} si esa etapa ya está configurada en ese flujo */
    boolean existsByFlujoIdFlujoAndEtapaIdEtapa(Long idFlujo, Long idEtapa);

    /** REQ-NF-018: ¿la etapa actual de un pedido es la etapa final de su flujo? Usado para bloquear la supresión de datos mientras el pedido sigue en curso. */
    boolean existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue(Long idFlujo, Long idEtapa);

    /** @return {@code true} si ya existe una etapa en esa posición del flujo */
    boolean existsByFlujoIdFlujoAndNumeroOrden(Long idFlujo, Integer numeroOrden);

    /** Elimina toda la configuración de etapas de un flujo. */
    void deleteByFlujoIdFlujo(Long idFlujo);
}

