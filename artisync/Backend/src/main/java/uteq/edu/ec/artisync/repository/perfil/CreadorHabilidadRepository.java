package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.CreadorHabilidad;

import java.util.List;

@Repository
public interface CreadorHabilidadRepository extends JpaRepository<CreadorHabilidad, Long> {
    List<CreadorHabilidad> findByPerfilIdPerfil(Long idPerfil);
}
