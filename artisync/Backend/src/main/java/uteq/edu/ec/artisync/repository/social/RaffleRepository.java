package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.Raffle;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Raffle}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface RaffleRepository extends JpaRepository<Raffle, Long>, RaffleRepositoryCustom {

    /** Ya existía — usado internamente. Mantenido por compatibilidad. */
    List<Raffle> findByFechaCierreLessThanEqualAndEstadoSorteo(LocalDateTime fecha, String estadoSorteo);

    /** Usado por el Scheduler para obtener sorteos "Activos" cuya fecha de cierre ya pasó. */
    List<Raffle> findByEstadoSorteoAndFechaCierreBefore(String estadoSorteo, LocalDateTime ahora);

    /** Sorteos públicos de un creador específico. */
    List<Raffle> findByPerfilCreadorIdPerfil(Long idPerfil);

    /** Todos los sorteos con estado "Activo" (listado público). */
    List<Raffle> findByEstadoSorteo(String estadoSorteo);
}



