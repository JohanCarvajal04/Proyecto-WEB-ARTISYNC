package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.DatosPagoCreador;

import java.util.Optional;

@Repository
public interface DatosPagoCreadorRepository extends JpaRepository<DatosPagoCreador, Long> {

    Optional<DatosPagoCreador> findByUsuarioIdUsuario(Long idUsuario);
}
