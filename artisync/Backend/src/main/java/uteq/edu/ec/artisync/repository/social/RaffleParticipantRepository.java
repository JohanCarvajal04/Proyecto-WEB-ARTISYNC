package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.RaffleParticipant;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link RaffleParticipant}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface RaffleParticipantRepository extends JpaRepository<RaffleParticipant, Long> {

    /**
     * Verifica si un usuario ya está inscrito en un sorteo (para evitar duplicados).
     * @param idSorteo el identificador de sorteo
     * @param idUsuario el identificador de usuario
     * @return true si ya existe, false en caso contrario
     */
    boolean existsBySorteoIdSorteoAndUsuarioIdUsuario(Long idSorteo, Long idUsuario);

    /**
     * Lista todos los participantes de un sorteo (ganadores y no ganadores).
     * @param idSorteo el identificador de sorteo
     * @return la lista de RaffleParticipant encontrados
     */
    List<RaffleParticipant> findBySorteoIdSorteo(Long idSorteo);

    /**
     * Lista solo los participantes que aún NO han sido marcados como ganadores.
     * @param idSorteo el identificador de sorteo
     * @return la lista de RaffleParticipant encontrados
     */
    List<RaffleParticipant> findBySorteoIdSorteoAndEsGanadorFalse(Long idSorteo);

    /**
     * Lista los ganadores de un sorteo.
     * @param idSorteo el identificador de sorteo
     * @return la lista de RaffleParticipant encontrados
     */
    List<RaffleParticipant> findBySorteoIdSorteoAndEsGanadorTrue(Long idSorteo);

    /**
     * Verifica si un sorteo tiene al menos un participante (restricción de edición).
     * @param idSorteo el identificador de sorteo
     * @return true si ya existe, false en caso contrario
     */
    boolean existsBySorteoIdSorteo(Long idSorteo);
}

