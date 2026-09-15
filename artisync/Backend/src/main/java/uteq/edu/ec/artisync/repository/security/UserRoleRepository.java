package uteq.edu.ec.artisync.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.UserRole;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link UserRole}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long>, UserRoleRepositoryCustom {

    /** Roles asignados a un usuario. */
    List<UserRole> findByUsuarioIdUsuario(Long idUsuario);

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8) - carga en
     * UNA sola consulta los roles (con Role.permisos, ya EAGER) de TODOS los
     * usuarios de {@code idsUsuario}. La usa UserMapper.toUserResponseList
     * para eliminar el N+1 de invocar findByUsuarioIdUsuario por cada fila de
     * una pagina de administracion de usuarios.
     */
    List<UserRole> findByUsuarioIdUsuarioIn(List<Long> idsUsuario);

    /** Roles asignados a un usuario, identificado por su correo. */
    List<UserRole> findByUsuarioCorreo(String correo);

    /** @return {@code true} si algún usuario tiene asignado ese rol (bloquea su eliminación) */
    boolean existsByRolIdRol(Long idRol);

    /**
     * Ids de los usuarios que tienen un rol dado. Se usa al sincronizar los
     * permisos de un rol para revocar sus sesiones: los permisos viajan en el
     * claim `permisos` del JWT, así que sin revocar seguirían operando con los
     * permisos antiguos hasta que el token caducara.
     */
    @Query("SELECT ur.usuario.idUsuario FROM UserRole ur WHERE UPPER(ur.rol.nombreRol) = UPPER(:nombreRol)")
    List<Long> findIdsUsuarioByNombreRol(@Param("nombreRol") String nombreRol);
}


