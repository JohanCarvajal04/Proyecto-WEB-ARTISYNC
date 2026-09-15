package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

/**
 * Invoca {@code fn_revocar_sesiones_usuario} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita. La función
 * devuelve una tabla ({@code jti VARCHAR(36), segundos_restantes INTEGER}); se mapea fila a fila
 * a {@link RevokedSessionProjection} con un {@code RowMapper} explícito.
 */
@RequiredArgsConstructor
public class UserSessionRepositoryImpl implements UserSessionRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private record RevokedSessionRow(String jti, Integer segundosRestantes) implements RevokedSessionProjection {

        @Override
        public String getJti() {
            return jti;
        }

        @Override
        public Integer getSegundosRestantes() {
            return segundosRestantes;
        }
    }

    /**
     * @param idUsuario identificador del usuario cuyas sesiones se revocan
     * @return las sesiones revocadas (jti + segundos restantes de vigencia)
     */
    @Override
    public List<RevokedSessionProjection> revocarSesionesUsuario(Long idUsuario) {
        MapSqlParameterSource params = new MapSqlParameterSource("p_id_usuario", idUsuario);
        return jdbcTemplate.query(
                "SELECT * FROM fn_revocar_sesiones_usuario(:p_id_usuario)",
                params,
                (rs, rowNum) -> new RevokedSessionRow(
                        rs.getString("jti"),
                        rs.getObject("segundos_restantes", Integer.class)));
    }
}
