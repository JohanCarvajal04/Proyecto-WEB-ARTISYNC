package uteq.edu.ec.artisync.repository.security;

/**
 * Invocaciones de {@code fn_sincronizar_permisos_rol}, {@code fn_eliminar_rol} y
 * {@code fn_crear_rol} vía JDBC directo, fuera del mecanismo de {@code @Query} de Spring Data
 * JPA (ver {@link RoleRepositoryImpl}).
 */
public interface RoleRepositoryCustom {

    /** REQ-F-003 - fn_sincronizar_permisos_rol: reemplaza atómicamente el set de permisos de un rol. Devuelve el total asignado. */
    Integer sincronizarPermisos(String nombreRol, String[] codigosPermiso);

    /** REQ-F-004 - fn_eliminar_rol: elimina un rol solo si no es protegido y no tiene usuarios asignados. */
    Boolean eliminarRol(Long idRol);

    /** fn_crear_rol: crea un rol y asigna sus permisos iniciales atómicamente. Devuelve el id_rol generado. */
    Long crearRol(String nombreRol, String descripcionRol, String[] codigosPermiso);
}
