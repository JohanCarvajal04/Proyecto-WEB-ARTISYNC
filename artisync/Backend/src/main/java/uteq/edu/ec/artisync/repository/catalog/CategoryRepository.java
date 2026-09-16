package uteq.edu.ec.artisync.repository.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalog.Category;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Category}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Categorías activas, para el catálogo público, ordenadas alfabéticamente.
     * @return la lista de Category encontrados
     */
    List<Category> findByEstadoActivaTrueOrderByNombreCategoriaAsc();

    /**
     * Todas las categorías (incluidas inactivas), para administración, ordenadas alfabéticamente.
     * @return la lista de Category encontrados
     */
    List<Category> findAllByOrderByNombreCategoriaAsc();

    /**
     * Categoría por nombre exacto, sin distinguir mayúsculas/minúsculas.
     * @param nombreCategoria el nombre de categoria
     * @return un Optional con Category si existe, vacio en caso contrario
     */
    Optional<Category> findByNombreCategoriaIgnoreCase(String nombreCategoria);

    /**
     * @param nombreCategoria el nombre de categoria
     * @return {@code true} si ya existe una categoría con ese nombre (sin distinguir mayúsculas/minúsculas)
     */
    boolean existsByNombreCategoriaIgnoreCase(String nombreCategoria);

    /**
     * Categorías creadas por un creador aún sin revisar por un moderador, más recientes primero.
     * @return la lista de Category encontrados
     */
    List<Category> findByRevisadoFalseOrderByActualizadoEnDesc();
}


