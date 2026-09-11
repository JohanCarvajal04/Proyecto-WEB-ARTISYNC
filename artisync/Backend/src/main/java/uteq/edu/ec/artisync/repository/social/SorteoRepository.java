package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.Sorteo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Sorteo}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface SorteoRepository extends JpaRepository<Sorteo, Long> {

    /** Ya existÃ­a â€” usado internamente. Mantenido por compatibilidad. */
    List<Sorteo> findByFechaCierreLessThanEqualAndEstadoSorteo(LocalDateTime fecha, String estadoSorteo);

    /** Usado por el Scheduler para obtener sorteos "Activos" cuya fecha de cierre ya pasÃ³. */
    List<Sorteo> findByEstadoSorteoAndFechaCierreBefore(String estadoSorteo, LocalDateTime ahora);

    /** Sorteos pÃºblicos de un creador especÃ­fico. */
    List<Sorteo> findByPerfilCreadorIdPerfil(Long idPerfil);

    /** Todos los sorteos con estado "Activo" (listado pÃºblico). */
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



