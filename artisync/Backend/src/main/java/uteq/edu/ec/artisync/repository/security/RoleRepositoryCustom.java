package uteq.edu.ec.artisync.repository.security;

/**
 * Invocaciones de {@code fn_sincronizar_permisos_rol}, {@code fn_eliminar_rol} y
 * {@code fn_crear_rol} vía JDBC directo, fuera del mecanismo de {@code @Query} de Spring Data
 * JPA (ver {@link RoleRepositoryImpl}).
 */
public interface RoleRepositoryCustom {

    /**
     * REQ-F-003 - fn_sincronizar_permisos_rol: reemplaza atómicamente el set de permisos de un rol. Devuelve el total asignado.
     * @param nombreRol el nombre de rol
     * @param codigosPermiso el codigos permiso
     * @return el valor numerico calculado
     */
    Integer sincronizarPermisos(String nombreRol, String[] codigosPermiso);

    /**
     * REQ-F-004 - fn_eliminar_rol: elimina un rol solo si no es protegido y no tiene usuarios asignados.
     * @param idRol el identificador de rol
     * @return true o false segun el resultado de la operacion
     */
    Boolean eliminarRol(Long idRol);

    /**
     * fn_crear_rol: crea un rol y asigna sus permisos iniciales atómicamente. Devuelve el id_rol generado.
     * @param nombreRol el nombre de rol
     * @param descripcionRol el descripcion rol
     * @param codigosPermiso el codigos permiso
     * @return el valor numerico calculado
     */
    Long crearRol(String nombreRol, String descripcionRol, String[] codigosPermiso);
}
