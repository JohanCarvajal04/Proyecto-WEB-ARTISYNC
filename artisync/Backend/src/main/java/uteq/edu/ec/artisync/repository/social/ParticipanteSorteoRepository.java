package uteq.edu.ec.artisync.repository.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.social.ParticipanteSorteo;

import java.util.List;

/**
 * Repositorio de participantes en sorteos.
 * RF-23: Gestiona inscripciones, validaciones de unicidad y selección de ganadores.
 */
@Repository
public interface ParticipanteSorteoRepository extends JpaRepository<ParticipanteSorteo, Long> {

    /** Verifica si un usuario ya está inscrito en un sorteo (para evitar duplicados). */
    boolean existsBySorteoIdSorteoAndUsuarioIdUsuario(Long idSorteo, Long idUsuario);

    /** Lista todos los participantes de un sorteo (ganadores y no ganadores). */
    List<ParticipanteSorteo> findBySorteoIdSorteo(Long idSorteo);

    /** Lista solo los participantes que aún NO han sido marcados como ganadores. */
    List<ParticipanteSorteo> findBySorteoIdSorteoAndEsGanadorFalse(Long idSorteo);

    /** Lista los ganadores de un sorteo. */
    List<ParticipanteSorteo> findBySorteoIdSorteoAndEsGanadorTrue(Long idSorteo);

    /** Verifica si un sorteo tiene al menos un participante (restricción de edición). */
    boolean existsBySorteoIdSorteo(Long idSorteo);
}
