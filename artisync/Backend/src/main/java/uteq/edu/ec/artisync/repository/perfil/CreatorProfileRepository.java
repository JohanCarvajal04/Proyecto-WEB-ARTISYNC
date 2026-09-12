package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link CreatorProfile}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, Long> {

    /** El perfil de creador de un usuario, si lo tiene. */
    Optional<CreatorProfile> findByUsuarioIdUsuario(Long idUsuario);

    /** Directorio público: solo creadores cuya cuenta no está suspendida. */
    List<CreatorProfile> findByUsuarioEstadoCuentaTrue();
}

