package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    /** La plantilla más reciente (mayor id), usada para validar duplicados al crear una nueva versión. */
    Optional<ContractTemplate> findFirstByOrderByIdPlantillaDesc();

    /** Plantilla por su versión legal exacta. */
    Optional<ContractTemplate> findByVersionLegal(String versionLegal);

    /** Fallback usado por ContractServiceImpl cuando el servicio no tiene una plantilla propia. */
    Optional<ContractTemplate> findByEsPredeterminadaTrue();

    /** Plantillas activas del catálogo general, ordenadas alfabéticamente. */
    List<ContractTemplate> findByActivaTrueOrderByNombrePlantillaAsc();

    /** Plantillas privadas (V45) de un creador, activas e inactivas — para su propia pantalla de gestión. */
    List<ContractTemplate> findByIdCreadorOrderByNombrePlantillaAsc(Long idCreador);

    /** Ownership check: la plantilla debe existir Y ser privada de ese creador. */
    Optional<ContractTemplate> findByIdPlantillaAndIdCreador(Long idPlantilla, Long idCreador);

    /**
     * Plantillas activas visibles para el selector del creador al crear/editar
     * su servicio: el catálogo general (id_creador NULL) más sus propias
     * plantillas privadas (V45). Nunca las plantillas privadas de otro creador.
     */
    @Query("SELECT p FROM ContractTemplate p WHERE p.activa = true AND (p.idCreador IS NULL OR p.idCreador = :idCreador) ORDER BY p.nombrePlantilla ASC")
    List<ContractTemplate> findActivasVisiblesParaCreador(@Param("idCreador") Long idCreador);
}
