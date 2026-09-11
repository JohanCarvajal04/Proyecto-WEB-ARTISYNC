package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PlantillaContrato}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface PlantillaContratoRepository extends JpaRepository<PlantillaContrato, Long> {

    Optional<PlantillaContrato> findFirstByOrderByIdPlantillaDesc();

    Optional<PlantillaContrato> findByVersionLegal(String versionLegal);

    /** Fallback usado por ContratoServicioImpl cuando el servicio no tiene una plantilla propia. */
    Optional<PlantillaContrato> findByEsPredeterminadaTrue();

    List<PlantillaContrato> findByActivaTrueOrderByNombrePlantillaAsc();
}

