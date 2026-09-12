package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.DynamicAttribute;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link DynamicAttribute}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface DynamicAttributeRepository extends JpaRepository<DynamicAttribute, Long> {

    /** Atributo dinámico por nombre exacto, sin distinguir mayúsculas/minúsculas. */
    Optional<DynamicAttribute> findByNombreAtributoIgnoreCase(String nombreAtributo);

    /** @return {@code true} si ya existe un atributo con ese nombre (sin distinguir mayúsculas/minúsculas) */
    boolean existsByNombreAtributoIgnoreCase(String nombreAtributo);
}


