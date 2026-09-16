package uteq.edu.ec.artisync.repository.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalog.OfferingSubcategory;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link OfferingSubcategory}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface OfferingSubcategoryRepository extends JpaRepository<OfferingSubcategory, Long> {

    /**
     * Subcategorías asignadas a un servicio.
     * @param idServicio el identificador de servicio
     * @return la lista de OfferingSubcategory encontrados
     */
    List<OfferingSubcategory> findByServicioIdServicio(Long idServicio);

    /**
     * Subcategorías asignadas a cualquiera de los servicios indicados.
     * @param idsServicio el ids servicio
     * @return la lista de OfferingSubcategory encontrados
     */
    List<OfferingSubcategory> findByServicioIdServicioIn(List<Long> idsServicio);

    /**
     * Elimina todas las asignaciones de subcategoría de un servicio.
     * @param idServicio el identificador de servicio
     */
    void deleteByServicioIdServicio(Long idServicio);

    /**
     * Elimina la asignación puntual de una subcategoría a un servicio.
     * @param idServicio el identificador de servicio
     * @param idSubcategoria el identificador de subcategoria
     */
    void deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(Long idServicio, Long idSubcategoria);

    /**
     * Cantidad de subcategorías asignadas a un servicio.
     * @param idServicio el identificador de servicio
     * @return el valor numerico calculado
     */
    long countByServicioIdServicio(Long idServicio);

    /**
     * @param idSubcategoria el identificador de subcategoria
     * @return {@code true} si alguna oferta usa esa subcategoría (bloquea su eliminación)
     */
    boolean existsBySubcategoriaIdSubcategoria(Long idSubcategoria);

    /**
     * @param idCategoria el identificador de categoria
     * @return {@code true} si alguna oferta usa una subcategoría de esa categoría
     */
    boolean existsBySubcategoriaCategoriaIdCategoria(Long idCategoria);
}


