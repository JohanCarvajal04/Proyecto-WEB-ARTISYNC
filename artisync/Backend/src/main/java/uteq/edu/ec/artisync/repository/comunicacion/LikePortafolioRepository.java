package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.LikePortafolio;

import java.util.Optional;

@Repository
public interface LikePortafolioRepository extends JpaRepository<LikePortafolio, Long> {

    boolean existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(Long idItemPortafolio, Long idUsuario);

    Optional<LikePortafolio> findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(
            Long idItemPortafolio, Long idUsuario);

    long countByItemPortafolioIdItemPortafolio(Long idItemPortafolio);
}
