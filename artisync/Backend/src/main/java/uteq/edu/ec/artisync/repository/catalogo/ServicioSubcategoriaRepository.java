package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.ServicioSubcategoria;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link ServicioSubcategoria}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface ServicioSubcategoriaRepository extends JpaRepository<ServicioSubcategoria, Long> {

    List<ServicioSubcategoria> findByServicioIdServicio(Long idServicio);

    List<ServicioSubcategoria> findByServicioIdServicioIn(List<Long> idsServicio);

    void deleteByServicioIdServicio(Long idServicio);

    void deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(Long idServicio, Long idSubcategoria);

    long countByServicioIdServicio(Long idServicio);

    boolean existsBySubcategoriaIdSubcategoria(Long idSubcategoria);

    boolean existsBySubcategoriaCategoriaIdCategoria(Long idCategoria);
}


