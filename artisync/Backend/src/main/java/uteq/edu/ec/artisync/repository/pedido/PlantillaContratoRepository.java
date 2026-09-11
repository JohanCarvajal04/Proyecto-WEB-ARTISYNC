package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaContratoRepository extends JpaRepository<PlantillaContrato, Long> {

    Optional<PlantillaContrato> findFirstByOrderByIdPlantillaDesc();

    Optional<PlantillaContrato> findByVersionLegal(String versionLegal);

    /** Fallback usado por ContratoServicioImpl cuando el servicio no tiene una plantilla propia. */
    Optional<PlantillaContrato> findByEsPredeterminadaTrue();

    List<PlantillaContrato> findByActivaTrueOrderByNombrePlantillaAsc();

    /** Plantillas privadas (V45) de un creador, activas e inactivas — para su propia pantalla de gestión. */
    List<PlantillaContrato> findByIdCreadorOrderByNombrePlantillaAsc(Long idCreador);

    /** Ownership check: la plantilla debe existir Y ser privada de ese creador. */
    Optional<PlantillaContrato> findByIdPlantillaAndIdCreador(Long idPlantilla, Long idCreador);

    /**
     * Plantillas activas visibles para el selector del creador al crear/editar
     * su servicio: el catálogo general (id_creador NULL) más sus propias
     * plantillas privadas (V45). Nunca las plantillas privadas de otro creador.
     */
    @Query("SELECT p FROM PlantillaContrato p WHERE p.activa = true AND (p.idCreador IS NULL OR p.idCreador = :idCreador) ORDER BY p.nombrePlantilla ASC")
    List<PlantillaContrato> findActivasVisiblesParaCreador(@Param("idCreador") Long idCreador);
}
