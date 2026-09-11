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

    List<Subcategory> findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(Long idCategoria);

    List<Subcategory> findAllByOrderByNombreSubcategoriaAsc();

    Optional<Subcategory> findByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(Long idCategoria, String nombreSubcategoria);

    boolean existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(Long idCategoria, String nombreSubcategoria);

    List<Subcategory> findByRevisadoFalseOrderByActualizadoEnDesc();
}


