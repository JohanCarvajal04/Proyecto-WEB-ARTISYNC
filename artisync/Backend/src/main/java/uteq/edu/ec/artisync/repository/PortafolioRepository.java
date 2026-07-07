package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.Portafolio;

import java.util.Optional;

@Repository
public interface PortafolioRepository extends JpaRepository<Portafolio, Long> {

    Optional<Portafolio> findByPerfilIdPerfil(Long idPerfil);
}
