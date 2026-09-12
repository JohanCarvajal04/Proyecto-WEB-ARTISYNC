package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.OfferingSubcategory;

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

    /** Subcategorías asignadas a un servicio. */
    List<OfferingSubcategory> findByServicioIdServicio(Long idServicio);

    /** Subcategorías asignadas a cualquiera de los servicios indicados. */
    List<OfferingSubcategory> findByServicioIdServicioIn(List<Long> idsServicio);

    /** Elimina todas las asignaciones de subcategoría de un servicio. */
    void deleteByServicioIdServicio(Long idServicio);

    /** Elimina la asignación puntual de una subcategoría a un servicio. */
    void deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(Long idServicio, Long idSubcategoria);

    /** Cantidad de subcategorías asignadas a un servicio. */
    long countByServicioIdServicio(Long idServicio);

    /** @return {@code true} si alguna oferta usa esa subcategoría (bloquea su eliminación) */
    boolean existsBySubcategoriaIdSubcategoria(Long idSubcategoria);

    /** @return {@code true} si alguna oferta usa una subcategoría de esa categoría */
    boolean existsBySubcategoriaCategoriaIdCategoria(Long idCategoria);
}


