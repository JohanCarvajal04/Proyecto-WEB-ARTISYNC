package uteq.edu.ec.artisync.repository.sistema;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.sistema.RespaldoBd;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RespaldoBdRepository extends JpaRepository<RespaldoBd, Long> {

    Page<RespaldoBd> findByTipo(RespaldoBd.TipoRespaldo tipo, Pageable pageable);

    Page<RespaldoBd> findByCategoria(RespaldoBd.CategoriaRespaldo categoria, Pageable pageable);

    Page<RespaldoBd> findByTipoAndCategoria(RespaldoBd.TipoRespaldo tipo, RespaldoBd.CategoriaRespaldo categoria, Pageable pageable);

    List<RespaldoBd> findByEstadoAndFechaExpiracionBefore(RespaldoBd.EstadoRespaldo estado, LocalDateTime fecha);

    Optional<RespaldoBd> findByNombreArchivo(String nombreArchivo);

    long countByEstadoAndTipo(RespaldoBd.EstadoRespaldo estado, RespaldoBd.TipoRespaldo tipo);
    long countByEstadoAndCategoria(RespaldoBd.EstadoRespaldo estado, RespaldoBd.CategoriaRespaldo categoria);
    long countByEstado(RespaldoBd.EstadoRespaldo estado);

    Optional<RespaldoBd> findTopByEstadoAndTipoOrderByFechaCreacionDesc(RespaldoBd.EstadoRespaldo estado, RespaldoBd.TipoRespaldo tipo);
    Optional<RespaldoBd> findTopByEstadoOrderByFechaCreacionDesc(RespaldoBd.EstadoRespaldo estado);

    /** Último respaldo AUTOMATICO exitoso de una categoría — para detectar si el
     *  disparador programado realmente corrió, sin que un respaldo manual de la
     *  misma categoría oculte que el automático lleva tiempo sin ejecutarse. */
    Optional<RespaldoBd> findTopByEstadoAndTipoAndCategoriaOrderByFechaCreacionDesc(
            RespaldoBd.EstadoRespaldo estado, RespaldoBd.TipoRespaldo tipo, RespaldoBd.CategoriaRespaldo categoria);
}
