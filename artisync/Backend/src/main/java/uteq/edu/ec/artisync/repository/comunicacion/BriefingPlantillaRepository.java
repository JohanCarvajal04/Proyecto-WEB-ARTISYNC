package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;

import java.util.List;

@Repository
public interface BriefingPlantillaRepository extends JpaRepository<BriefingPlantilla, Long> {

    List<BriefingPlantilla> findByPerfilCreadorIdPerfil(Long idPerfil);
}
