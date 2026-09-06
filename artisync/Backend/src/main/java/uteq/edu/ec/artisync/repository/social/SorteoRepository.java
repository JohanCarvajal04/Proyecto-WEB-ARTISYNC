package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * REQ-F-023 - fn_seleccionar_ganadores_sorteo: sortea ganadores y actualiza participantes+sorteo
     * en bloque. Devuelve JSONB serializado como texto.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra una FUNCTION de Postgres
     * (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC, invalida). Ver el
     * hallazgo completo en docs/basedatos/CATALOGO-SP.md ??14.
     */
    @Query(value = "SELECT fn_seleccionar_ganadores_sorteo(:p_id_sorteo)::text", nativeQuery = true)
    String seleccionarGanadores(@Param("p_id_sorteo") Long idSorteo);
}


