package uteq.edu.ec.artisync.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.Role;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Role}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, RoleRepositoryCustom {

    /** Rol por nombre exacto. */
    Optional<Role> findByNombreRol(String nombreRol);
}


