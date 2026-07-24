package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingRespuesta;

import java.util.List;

@Repository
public interface BriefingRespuestaRepository extends JpaRepository<BriefingRespuesta, Long> {

    List<BriefingRespuesta> findByBriefingEnviadoIdBriefingEnviado(Long idBriefingEnviado);

    boolean existsByBriefingEnviadoIdBriefingEnviadoAndPreguntaIdPregunta(Long idBriefingEnviado, Long idPregunta);
}
