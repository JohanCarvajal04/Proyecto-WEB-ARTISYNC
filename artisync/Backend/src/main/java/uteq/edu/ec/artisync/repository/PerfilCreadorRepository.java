package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;

import java.util.Optional;

@Repository
public interface PerfilCreadorRepository extends JpaRepository<PerfilCreador, Long> {

    Optional<PerfilCreador> findByUsuarioIdUsuario(Long idUsuario);
}
