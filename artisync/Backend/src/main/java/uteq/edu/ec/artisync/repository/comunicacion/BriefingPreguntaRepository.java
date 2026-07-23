package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPregunta;

import java.util.List;

@Repository
public interface BriefingPreguntaRepository extends JpaRepository<BriefingPregunta, Long> {

    List<BriefingPregunta> findByPlantillaIdBriefingPlantillaOrderByNumeroOrdenAsc(Long idPlantilla);

    long countByPlantillaIdBriefingPlantilla(Long idPlantilla);
}
