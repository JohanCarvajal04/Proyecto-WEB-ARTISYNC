package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.SesionUsuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {

    Optional<SesionUsuario> findByTokenJwt(String tokenJwt);

    List<SesionUsuario> findByUsuarioIdUsuario(Long idUsuario);

    void deleteByUsuarioIdUsuario(Long idUsuario);
}
