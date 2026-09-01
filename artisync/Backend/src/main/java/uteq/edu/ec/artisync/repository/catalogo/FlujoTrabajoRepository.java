package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlujoTrabajoRepository extends JpaRepository<FlujoTrabajo, Long> {

    List<FlujoTrabajo> findByCreadorIdUsuario(Long idUsuario);

    /** Para quien tiene FLUJO_MODERAR: todos los flujos, de cualquier creador. */
    List<FlujoTrabajo> findAllByOrderByIdFlujoAsc();

    Optional<FlujoTrabajo> findByIdFlujoAndCreadorIdUsuario(Long idFlujo, Long idUsuario);

    boolean existsByNombreFlujoAndCreadorIdUsuario(String nombreFlujo, Long idUsuario);

    boolean existsByNombreFlujoAndCreadorIdUsuarioAndIdFlujoNot(String nombreFlujo, Long idUsuario, Long idFlujo);

    /**
     * Flujo de menor id para un creador.
     */
    Optional<FlujoTrabajo> findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(Long idUsuario);

    /**
     * Flujo de menor id. Sirve de respaldo.
     */
    Optional<FlujoTrabajo> findFirstByOrderByIdFlujoAsc();
}
