package uteq.edu.ec.artisync.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.order.ContractTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link ContractTemplate}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Combina Derived Queries con una consulta JPQL mediante
 * {@code @Query} para resolver el catálogo de plantillas visibles para un creador.
 */
@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    /**
     * La plantilla más reciente (mayor id), usada para validar duplicados al crear una nueva versión.
     * @return un Optional con ContractTemplate si existe, vacio en caso contrario
     */
    Optional<ContractTemplate> findFirstByOrderByIdPlantillaDesc();

    /**
     * Plantilla por su versión legal exacta.
     * @param versionLegal el version legal
     * @return un Optional con ContractTemplate si existe, vacio en caso contrario
     */
    Optional<ContractTemplate> findByVersionLegal(String versionLegal);

    /**
     * Fallback usado por ContractServiceImpl cuando el servicio no tiene una plantilla propia.
     * @return un Optional con ContractTemplate si existe, vacio en caso contrario
     */
    Optional<ContractTemplate> findByEsPredeterminadaTrue();

    /**
     * Plantillas activas del catálogo general, ordenadas alfabéticamente.
     * @return la lista de ContractTemplate encontrados
     */
    List<ContractTemplate> findByActivaTrueOrderByNombrePlantillaAsc();

    /**
     * Plantillas privadas (V45) de un creador, activas e inactivas — para su propia pantalla de gestión.
     * @param idCreador el identificador de creador
     * @return la lista de ContractTemplate encontrados
     */
    List<ContractTemplate> findByIdCreadorOrderByNombrePlantillaAsc(Long idCreador);

    /**
     * Ownership check: la plantilla debe existir Y ser privada de ese creador.
     * @param idPlantilla el identificador de plantilla
     * @param idCreador el identificador de creador
     * @return un Optional con ContractTemplate si existe, vacio en caso contrario
     */
    Optional<ContractTemplate> findByIdPlantillaAndIdCreador(Long idPlantilla, Long idCreador);

    /**
     * Plantillas activas visibles para el selector del creador al crear/editar
     * su servicio: el catálogo general (id_creador NULL) más sus propias
     * plantillas privadas (V45). Nunca las plantillas privadas de otro creador.
     * @param idCreador el identificador de creador
     * @return la lista de ContractTemplate encontrados
     */
    @Query("SELECT p FROM ContractTemplate p WHERE p.activa = true AND (p.idCreador IS NULL OR p.idCreador = :idCreador) ORDER BY p.nombrePlantilla ASC")
    List<ContractTemplate> findActivasVisiblesParaCreador(@Param("idCreador") Long idCreador);
}
