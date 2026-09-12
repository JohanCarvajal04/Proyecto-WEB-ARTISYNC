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

    /**
     * Igual que {@code findAll(spec, pageable)} estándar, pero con fetch join
     * de {@code perfil}/{@code perfil.usuario} para evitar el N+1 al mapear
     * cada fila del catálogo a su respuesta (que incluye datos del creador).
     *
     * @param spec filtros dinámicos a aplicar
     * @param pageable configuración de paginación y ordenamiento
     * @return la página de servicios con el perfil del creador ya cargado
     */
    @Override
    @EntityGraph(attributePaths = {"perfil", "perfil.usuario"})
    Page<Offering> findAll(Specification<Offering> spec, Pageable pageable);

    /** Servicios publicados por un perfil de creador. */
    List<Offering> findByPerfilIdPerfil(Long idPerfil);

    /** Servicios de un perfil de creador en un estado de publicación dado. */
    List<Offering> findByPerfilIdPerfilAndEstadoPublicacion(Long idPerfil, String estadoPublicacion);
}


