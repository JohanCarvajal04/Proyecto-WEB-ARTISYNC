package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    Optional<Seguidor> findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    boolean existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    List<Seguidor> findByPerfilCreadorIdPerfil(Long idPerfil);

    long countByPerfilCreadorIdPerfil(Long idPerfil);

    List<Seguidor> findByUsuarioSeguidorIdUsuario(Long idUsuario);
}
