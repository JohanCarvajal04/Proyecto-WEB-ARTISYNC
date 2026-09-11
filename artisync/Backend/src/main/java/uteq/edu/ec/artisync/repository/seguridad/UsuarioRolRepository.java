package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link UsuarioRol}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    List<UsuarioRol> findByUsuarioIdUsuario(Long idUsuario);

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§8) - carga en
     * UNA sola consulta los roles (con Rol.permisos, ya EAGER) de TODOS los
     * usuarios de {@code idsUsuario}. La usa UsuarioMapper.toUserResponseList
     * para eliminar el N+1 de invocar findByUsuarioIdUsuario por cada fila de
     * una pagina de administracion de usuarios.
     */
    List<UsuarioRol> findByUsuarioIdUsuarioIn(List<Long> idsUsuario);

    List<UsuarioRol> findByUsuarioCorreo(String correo);

    boolean existsByRolIdRol(Long idRol);

    /**
     * Ids de los usuarios que tienen un rol dado. Se usa al sincronizar los
     * permisos de un rol para revocar sus sesiones: los permisos viajan en el
     * claim `permisos` del JWT, asÃ­ que sin revocar seguirÃ­an operando con los
     * permisos antiguos hasta que el token caducara.
     */
    @Query("SELECT ur.usuario.idUsuario FROM UsuarioRol ur WHERE UPPER(ur.rol.nombreRol) = UPPER(:nombreRol)")
    List<Long> findIdsUsuarioByNombreRol(@Param("nombreRol") String nombreRol);

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§3) -
     * fn_sincronizar_roles_usuario: reemplaza atomicamente el set completo de
     * roles de un usuario (DELETE+INSERT con ON CONFLICT), serializado con
     * SELECT ... FOR UPDATE sobre usuarios. Gemela de
     * RolRepository.sincronizarPermisos (REQ-F-003) para el lado usuario<->rol.
     * Devuelve el total de filas insertadas.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra una FUNCTION de Postgres
     * (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC, invalida). Ver el
     * hallazgo completo en docs/basedatos/CATALOGO-SP.md ??14.
     */
    @Query(value = "SELECT fn_sincronizar_roles_usuario(:p_id_usuario, :p_nombres_rol)", nativeQuery = true)
    Integer sincronizarRoles(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_nombres_rol") String[] nombresRol);
}


