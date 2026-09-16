package uteq.edu.ec.artisync.repository.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.communication.Follower;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Follower}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface FollowerRepository extends JpaRepository<Follower, Long>, FollowerRepositoryCustom {

    /**
     * Relación de seguimiento entre un usuario y un perfil de creador, si existe.
     * @param idUsuario el identificador de usuario
     * @param idPerfil el identificador de perfil
     * @return un Optional con Follower si existe, vacio en caso contrario
     */
    Optional<Follower> findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    /**
     * @param idUsuario el identificador de usuario
     * @param idPerfil el identificador de perfil
     * @return {@code true} si el usuario ya sigue a ese perfil de creador
     */
    boolean existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    /**
     * Seguidores de un perfil de creador.
     * @param idPerfil el identificador de perfil
     * @return la lista de Follower encontrados
     */
    List<Follower> findByPerfilCreadorIdPerfil(Long idPerfil);

    /**
     * Cantidad de seguidores de un perfil de creador.
     * @param idPerfil el identificador de perfil
     * @return el valor numerico calculado
     */
    long countByPerfilCreadorIdPerfil(Long idPerfil);

    /**
     * Perfiles de creador que sigue un usuario.
     * @param idUsuario el identificador de usuario
     * @return la lista de Follower encontrados
     */
    List<Follower> findByUsuarioSeguidorIdUsuario(Long idUsuario);
}



