package uteq.edu.ec.artisync.repository.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.communication.PortfolioLike;

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

    /**
     * @param idItemPortafolio el identificador de item portafolio
     * @param idUsuario el identificador de usuario
     * @return {@code true} si ese usuario ya dio like a esa obra del portafolio
     */
    boolean existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(Long idItemPortafolio, Long idUsuario);

    /**
     * El like de un usuario sobre una obra del portafolio, si existe (para poder quitarlo).
     * @param idItemPortafolio el identificador de item portafolio
     * @param idUsuario el identificador de usuario
     * @return un Optional con PortfolioLike si existe, vacio en caso contrario
     */
    Optional<PortfolioLike> findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(
            Long idItemPortafolio, Long idUsuario);

    /**
     * Cantidad de likes de una obra del portafolio.
     * @param idItemPortafolio el identificador de item portafolio
     * @return el valor numerico calculado
     */
    long countByItemPortafolioIdItemPortafolio(Long idItemPortafolio);
}


