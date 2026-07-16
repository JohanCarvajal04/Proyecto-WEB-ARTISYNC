package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;

import java.util.Optional;

@Repository
public interface FlujoTrabajoRepository extends JpaRepository<FlujoTrabajo, Long> {

    Optional<FlujoTrabajo> findByNombreFlujo(String nombreFlujo);

    boolean existsByNombreFlujo(String nombreFlujo);
}
