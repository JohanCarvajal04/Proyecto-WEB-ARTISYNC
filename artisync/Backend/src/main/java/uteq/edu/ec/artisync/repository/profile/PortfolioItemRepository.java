package uteq.edu.ec.artisync.repository.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.profile.PortfolioItem;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PortfolioItem}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    /**
     * Obras de un portafolio, más recientes primero.
     * @param idPortafolio el identificador de portafolio
     * @return la lista de PortfolioItem encontrados
     */
    List<PortfolioItem> findByPortafolioIdPortafolioOrderByFechaSubidaDesc(Long idPortafolio);

    /**
     * Cantidad de obras de un portafolio.
     * @param idPortafolio el identificador de portafolio
     * @return el valor numerico calculado
     */
    long countByPortafolioIdPortafolio(Long idPortafolio);
}


