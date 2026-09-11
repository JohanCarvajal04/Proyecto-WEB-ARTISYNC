package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Seguidor}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    Optional<Seguidor> findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    boolean existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    List<Seguidor> findByPerfilCreadorIdPerfil(Long idPerfil);

    long countByPerfilCreadorIdPerfil(Long idPerfil);

    List<Seguidor> findByUsuarioSeguidorIdUsuario(Long idUsuario);

    /**
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Las 4 rutinas de abajo (fn_seguir_creador, fn_dejar_de_seguir_creador, fn_es_seguidor,
     * fn_conteo_seguidores) tienen retorno no-void: @Procedure rompe con Hibernate 7.4.1 contra una
     * FUNCTION de Postgres (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC,
     * invalida). Ver el hallazgo completo en docs/basedatos/CATALOGO-SP.md ??14.
     */
    @Query(value = "SELECT fn_seguir_creador(:idUsuario, :idPerfil)", nativeQuery = true)
    Boolean ejecutarFnSeguirCreador(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    @Query(value = "SELECT fn_dejar_de_seguir_creador(:idUsuario, :idPerfil)", nativeQuery = true)
    Boolean ejecutarFnDejarDeSeguirCreador(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    @Query(value = "SELECT fn_es_seguidor(:idUsuario, :idPerfil)", nativeQuery = true)
    Boolean ejecutarFnEsSeguidor(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    @Query(value = "SELECT fn_conteo_seguidores(:idPerfil)", nativeQuery = true)
    Long ejecutarFnConteoSeguidores(@Param("idPerfil") Long idPerfil);
}



