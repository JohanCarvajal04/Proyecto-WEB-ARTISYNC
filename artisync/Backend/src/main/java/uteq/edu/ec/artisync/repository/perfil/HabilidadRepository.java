package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.Habilidad;

import java.util.Optional;

@Repository
public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {
    Optional<Habilidad> findByNombreHabilidad(String nombreHabilidad);
}
