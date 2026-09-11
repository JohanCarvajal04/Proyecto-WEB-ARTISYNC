package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Offering;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Offering}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Implementa consultas con soporte para paginación dinámica y ordenamiento estructurado.
 */
@Repository
public interface OfferingRepository extends JpaRepository<Offering, Long>, JpaSpecificationExecutor<Offering> {

    @Override
    @EntityGraph(attributePaths = {"perfil", "perfil.usuario"})
    Page<Offering> findAll(Specification<Offering> spec, Pageable pageable);

    List<Offering> findByPerfilIdPerfil(Long idPerfil);

    List<Offering> findByPerfilIdPerfilAndEstadoPublicacion(Long idPerfil, String estadoPublicacion);
}


