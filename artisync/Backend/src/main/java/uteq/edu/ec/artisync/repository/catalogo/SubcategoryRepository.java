package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Subcategory;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Subcategory}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    /** Subcategorías de una categoría, ordenadas alfabéticamente. */
    List<Subcategory> findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(Long idCategoria);

    /** Todas las subcategorías, ordenadas alfabéticamente. */
    List<Subcategory> findAllByOrderByNombreSubcategoriaAsc();

    /** Subcategoría por categoría y nombre exacto, sin distinguir mayúsculas/minúsculas. */
    Optional<Subcategory> findByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(Long idCategoria, String nombreSubcategoria);

    /** @return {@code true} si ya existe esa subcategoría en esa categoría (sin distinguir mayúsculas/minúsculas) */
    boolean existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(Long idCategoria, String nombreSubcategoria);

    /** Subcategorías creadas por un creador aún sin revisar por un moderador, más recientes primero. */
    List<Subcategory> findByRevisadoFalseOrderByActualizadoEnDesc();
}


