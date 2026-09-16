package uteq.edu.ec.artisync.repository.security;

/**
 * Invocación de {@code fn_sincronizar_roles_usuario} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link UserRoleRepositoryImpl}).
 */
public interface UserRoleRepositoryCustom {

    /**
     * fn_sincronizar_roles_usuario: reemplaza atómicamente el set completo de roles de un usuario. Devuelve el total insertado.
     * @param idUsuario el identificador de usuario
     * @param nombresRol el nombres rol
     * @return el valor numerico calculado
     */
    Integer sincronizarRoles(Long idUsuario, String[] nombresRol);
}
