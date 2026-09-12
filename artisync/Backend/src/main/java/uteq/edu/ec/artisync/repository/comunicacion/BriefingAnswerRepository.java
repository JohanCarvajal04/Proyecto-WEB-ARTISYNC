package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingAnswer;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link BriefingAnswer}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface BriefingAnswerRepository extends JpaRepository<BriefingAnswer, Long> {

    /** Respuestas de un briefing enviado. */
    List<BriefingAnswer> findByBriefingEnviadoIdBriefingEnviado(Long idBriefingEnviado);

    /** @return {@code true} si ya existe una respuesta a esa pregunta en ese briefing enviado */
    boolean existsByBriefingEnviadoIdBriefingEnviadoAndPreguntaIdPregunta(Long idBriefingEnviado, Long idPregunta);
}


