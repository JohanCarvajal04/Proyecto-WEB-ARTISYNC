package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link BriefingTemplate}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface BriefingTemplateRepository extends JpaRepository<BriefingTemplate, Long> {

    /** Cuestionarios (briefings) definidos por un perfil de creador. */
    List<BriefingTemplate> findByPerfilCreadorIdPerfil(Long idPerfil);

    /** Usado al asignar un cuestionario a un servicio: solo uno de los propios del creador. */
    Optional<BriefingTemplate> findByIdBriefingPlantillaAndPerfilCreadorIdPerfil(Long idBriefingPlantilla, Long idPerfil);
}

