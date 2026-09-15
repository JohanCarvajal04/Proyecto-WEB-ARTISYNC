package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uteq.edu.ec.artisync.repository.support.PgArrays;

/**
 * Invoca {@code fn_sincronizar_permisos_rol}, {@code fn_eliminar_rol} y {@code fn_crear_rol} con
 * {@link NamedParameterJdbcTemplate} en vez de {@code @Query(nativeQuery = true)} (P6 de la guía
 * del examen suspenso). Ver {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate
 * que esto evita.
 */
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param nombreRol nombre del rol a sincronizar
     * @param codigosPermiso códigos de permiso que debe tener el rol tras la sincronización
     * @return el total de permisos asignados
     */
    @Override
    public Integer sincronizarPermisos(String nombreRol, String[] codigosPermiso) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_nombre_rol", nombreRol)
                .addValue("p_codigos_permiso", PgArrays.textArray(jdbcTemplate, codigosPermiso));
        return jdbcTemplate.queryForObject(
                "SELECT fn_sincronizar_permisos_rol(:p_nombre_rol, :p_codigos_permiso)", params, Integer.class);
    }

    /**
     * @param idRol identificador del rol a eliminar
     * @return {@code true} si el rol se eliminó
     */
    @Override
    public Boolean eliminarRol(Long idRol) {
        MapSqlParameterSource params = new MapSqlParameterSource("p_id_rol", idRol);
        return jdbcTemplate.queryForObject("SELECT fn_eliminar_rol(:p_id_rol)", params, Boolean.class);
    }

    /**
     * @param nombreRol nombre del nuevo rol
     * @param descripcionRol descripción del rol
     * @param codigosPermiso códigos de permiso iniciales del rol
     * @return el id_rol generado
     */
    @Override
    public Long crearRol(String nombreRol, String descripcionRol, String[] codigosPermiso) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_nombre_rol", nombreRol)
                .addValue("p_descripcion_rol", descripcionRol)
                .addValue("p_codigos_permiso", PgArrays.textArray(jdbcTemplate, codigosPermiso));
        return jdbcTemplate.queryForObject(
                "SELECT fn_crear_rol(:p_nombre_rol, :p_descripcion_rol, :p_codigos_permiso)", params, Long.class);
    }
}
