package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.OfferingAttribute;

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

    long countByServicioIdServicio(Long idServicio);

    List<OfferingAttribute> findByServicioIdServicio(Long idServicio);

    Optional<OfferingAttribute> findByServicioIdServicioAndAtributoIdAtributo(Long idServicio, Long idAtributo);

    void deleteByServicioIdServicio(Long idServicio);
}


