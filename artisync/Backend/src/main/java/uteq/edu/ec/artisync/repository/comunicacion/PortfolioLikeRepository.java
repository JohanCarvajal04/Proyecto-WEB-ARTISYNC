package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.PortfolioLike;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PortfolioLike}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface PortfolioLikeRepository extends JpaRepository<PortfolioLike, Long> {

    /** @return {@code true} si ese usuario ya dio like a esa obra del portafolio */
    boolean existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(Long idItemPortafolio, Long idUsuario);

    /** El like de un usuario sobre una obra del portafolio, si existe (para poder quitarlo). */
    Optional<PortfolioLike> findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(
            Long idItemPortafolio, Long idUsuario);

    /** Cantidad de likes de una obra del portafolio. */
    long countByItemPortafolioIdItemPortafolio(Long idItemPortafolio);
}


