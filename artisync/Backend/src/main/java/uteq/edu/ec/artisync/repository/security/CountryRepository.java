package uteq.edu.ec.artisync.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.Country;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Country}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long>, CountryRepositoryCustom {
    /**
     * País por nombre exacto.
     * @param nombrePais el nombre de pais
     * @return un Optional con Country si existe, vacio en caso contrario
     */
    Optional<Country> findByNombrePais(String nombrePais);
    /**
     * Países activos, en el orden solicitado.
     * @param sort el sort
     * @return la lista de Country encontrados
     */
    List<Country> findByEstadoTrue(Sort sort);
}


