package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.TipoNotificacion;

import java.util.Optional;

@Repository
public interface TipoNotificacionRepository extends JpaRepository<TipoNotificacion, Long> {

    Optional<TipoNotificacion> findByNombreEvento(String nombreEvento);
}
