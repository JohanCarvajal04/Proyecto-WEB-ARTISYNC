package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Tag;

import java.util.List;
import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Tag}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** Etiqueta por nombre exacto, sin distinguir mayúsculas/minúsculas. */
    Optional<Tag> findByNombreEtiquetaIgnoreCase(String nombreEtiqueta);

    /** Etiquetas cuyo nombre está en la lista dada. */
    List<Tag> findByNombreEtiquetaIn(List<String> nombres);

    /** @return {@code true} si ya existe una etiqueta con ese nombre (sin distinguir mayúsculas/minúsculas) */
    boolean existsByNombreEtiquetaIgnoreCase(String nombreEtiqueta);
}


