package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.InfraccionMensaje;

import java.time.LocalDateTime;

@Repository
public interface InfraccionRepository extends JpaRepository<InfraccionMensaje, Long> {

    long countByUsuarioIdUsuarioAndFechaInfraccionAfter(Long idUsuario, LocalDateTime fecha);
}
