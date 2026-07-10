package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;

import java.util.Optional;

@Repository
public interface EstadoVerificacionRepository extends JpaRepository<EstadoVerificacion, Long> {
    Optional<EstadoVerificacion> findByNombreEstado(String nombreEstado);
}
