package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.Follower;

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
public interface FollowerRepository extends JpaRepository<Follower, Long> {

    /** Relación de seguimiento entre un usuario y un perfil de creador, si existe. */
    Optional<Follower> findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    /** @return {@code true} si el usuario ya sigue a ese perfil de creador */
    boolean existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    /** Seguidores de un perfil de creador. */
    List<Follower> findByPerfilCreadorIdPerfil(Long idPerfil);

    /** Cantidad de seguidores de un perfil de creador. */
    long countByPerfilCreadorIdPerfil(Long idPerfil);

    /** Perfiles de creador que sigue un usuario. */
    List<Follower> findByUsuarioSeguidorIdUsuario(Long idUsuario);

    /**
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Las 4 rutinas de abajo (fn_seguir_creador, fn_dejar_de_seguir_creador, fn_es_seguidor,
     * fn_conteo_seguidores) tienen retorno no-void: @Procedure rompe con Hibernate 7.4.1 contra una
     * FUNCTION de Postgres (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC,
     * invalida). Ver el hallazgo completo en docs/basedatos/CATALOGO-SP.md §14.
     *
     * @param idUsuario identificador del usuario que sigue
     * @param idPerfil identificador del perfil de creador a seguir
     * @return {@code true} si la operación se realizó
     */
    @Query(value = "SELECT fn_seguir_creador(:idUsuario, :idPerfil)", nativeQuery = true)
    Boolean ejecutarFnSeguirCreador(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    /**
     * @param idUsuario identificador del usuario que deja de seguir
     * @param idPerfil identificador del perfil de creador
     * @return {@code true} si la operación se realizó
     */
    @Query(value = "SELECT fn_dejar_de_seguir_creador(:idUsuario, :idPerfil)", nativeQuery = true)
    Boolean ejecutarFnDejarDeSeguirCreador(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    /**
     * @param idUsuario identificador del usuario
     * @param idPerfil identificador del perfil de creador
     * @return {@code true} si el usuario sigue a ese perfil de creador
     */
    @Query(value = "SELECT fn_es_seguidor(:idUsuario, :idPerfil)", nativeQuery = true)
    Boolean ejecutarFnEsSeguidor(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    /**
     * @param idPerfil identificador del perfil de creador
     * @return la cantidad de seguidores de ese perfil
     */
    @Query(value = "SELECT fn_conteo_seguidores(:idPerfil)", nativeQuery = true)
    Long ejecutarFnConteoSeguidores(@Param("idPerfil") Long idPerfil);
}



