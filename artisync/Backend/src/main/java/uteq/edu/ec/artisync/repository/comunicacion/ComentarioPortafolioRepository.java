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

    /**
     * Conteo público (badge de la obra): solo cuenta los activos. Contar todos
     * sin filtrar inflaba el número con comentarios ocultos por moderación o
     * borrados lógicamente por su autor, que no aparecen en el listado público.
     */
    long countByItemPortafolioIdItemPortafolioAndEstadoModeracion(Long idItem, String estadoModeracion);
}
