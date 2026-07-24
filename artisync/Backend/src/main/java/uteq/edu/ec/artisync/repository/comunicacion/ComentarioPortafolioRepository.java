package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.ComentarioPortafolio;

@Repository
public interface ComentarioPortafolioRepository extends JpaRepository<ComentarioPortafolio, Long> {

    Page<ComentarioPortafolio> findByItemPortafolioIdItemPortafolioAndEstadoModeracion(
            Long idItem, String estadoModeracion, Pageable pageable);

    long countByItemPortafolioIdItemPortafolio(Long idItem);
}
