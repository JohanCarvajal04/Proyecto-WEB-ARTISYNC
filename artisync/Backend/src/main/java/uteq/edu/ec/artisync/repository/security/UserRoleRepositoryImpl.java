package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uteq.edu.ec.artisync.repository.support.PgArrays;

/**
 * Invoca {@code fn_sincronizar_roles_usuario} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita.
 */
@RequiredArgsConstructor
public class UserRoleRepositoryImpl implements UserRoleRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idUsuario identificador del usuario
     * @param nombresRol nombres de los roles que debe tener el usuario tras la sincronización
     * @return el total de filas insertadas
     */
    @Override
    public Integer sincronizarRoles(Long idUsuario, String[] nombresRol) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_usuario", idUsuario)
                .addValue("p_nombres_rol", PgArrays.textArray(jdbcTemplate, nombresRol));
        return jdbcTemplate.queryForObject(
                "SELECT fn_sincronizar_roles_usuario(:p_id_usuario, :p_nombres_rol)", params, Integer.class);
    }
}
