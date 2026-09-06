package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;

import java.util.List;
import java.util.Optional;

@Repository
public interface BriefingPlantillaRepository extends JpaRepository<BriefingPlantilla, Long> {

    List<BriefingPlantilla> findByPerfilCreadorIdPerfil(Long idPerfil);

    /** Usado al asignar un cuestionario a un servicio: solo uno de los propios del creador. */
    Optional<BriefingPlantilla> findByIdBriefingPlantillaAndPerfilCreadorIdPerfil(Long idBriefingPlantilla, Long idPerfil);
}
