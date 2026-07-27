package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.Sorteo;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SorteoRepository extends JpaRepository<Sorteo, Long> {

    /** Ya existía — usado internamente. Mantenido por compatibilidad. */
    List<Sorteo> findByFechaCierreLessThanEqualAndEstadoSorteo(LocalDateTime fecha, String estadoSorteo);

    /** Usado por el Scheduler para obtener sorteos "Activos" cuya fecha de cierre ya pasó. */
    List<Sorteo> findByEstadoSorteoAndFechaCierreBefore(String estadoSorteo, LocalDateTime ahora);

    /** Sorteos públicos de un creador específico. */
    List<Sorteo> findByPerfilCreadorIdPerfil(Long idPerfil);

    /** Todos los sorteos con estado "Activo" (listado público). */
    List<Sorteo> findByEstadoSorteo(String estadoSorteo);
}

