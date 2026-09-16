package uteq.edu.ec.artisync.repository.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalog.OfferingAttribute;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link OfferingAttribute}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface OfferingAttributeRepository extends JpaRepository<OfferingAttribute, Long> {

    /**
     * Cantidad de atributos dinámicos asignados a un servicio.
     * @param idServicio el identificador de servicio
     * @return el valor numerico calculado
     */
    long countByServicioIdServicio(Long idServicio);

    /**
     * Atributos dinámicos (valor por atributo) asignados a un servicio.
     * @param idServicio el identificador de servicio
     * @return la lista de OfferingAttribute encontrados
     */
    List<OfferingAttribute> findByServicioIdServicio(Long idServicio);

    /**
     * Valor de un atributo dinámico puntual en un servicio, si está asignado.
     * @param idServicio el identificador de servicio
     * @param idAtributo el identificador de atributo
     * @return un Optional con OfferingAttribute si existe, vacio en caso contrario
     */
    Optional<OfferingAttribute> findByServicioIdServicioAndAtributoIdAtributo(Long idServicio, Long idAtributo);

    /**
     * Elimina todos los atributos dinámicos asignados a un servicio.
     * @param idServicio el identificador de servicio
     */
    void deleteByServicioIdServicio(Long idServicio);
}


