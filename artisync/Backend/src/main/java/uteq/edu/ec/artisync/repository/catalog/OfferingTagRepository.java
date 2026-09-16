package uteq.edu.ec.artisync.repository.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalog.OfferingTag;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link OfferingTag}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface OfferingTagRepository extends JpaRepository<OfferingTag, Long> {

    /**
     * Etiquetas asignadas a un servicio.
     * @param idServicio el identificador de servicio
     * @return la lista de OfferingTag encontrados
     */
    List<OfferingTag> findByServicioIdServicio(Long idServicio);

    /**
     * Etiquetas asignadas a cualquiera de los servicios indicados.
     * @param idsServicio el ids servicio
     * @return la lista de OfferingTag encontrados
     */
    List<OfferingTag> findByServicioIdServicioIn(List<Long> idsServicio);

    /**
     * Elimina todas las etiquetas asignadas a un servicio.
     * @param idServicio el identificador de servicio
     */
    void deleteByServicioIdServicio(Long idServicio);
}


