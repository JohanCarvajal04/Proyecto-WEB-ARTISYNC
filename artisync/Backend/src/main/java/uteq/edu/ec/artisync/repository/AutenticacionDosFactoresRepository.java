package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;

import java.util.Optional;

@Repository
public interface AutenticacionDosFactoresRepository extends JpaRepository<AutenticacionDosFactores, Long> {

    Optional<AutenticacionDosFactores> findByUsuarioIdUsuario(Long idUsuario);

    Optional<AutenticacionDosFactores> findByUsuarioCorreo(String correo);
}
