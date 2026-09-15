package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uteq.edu.ec.artisync.repository.support.PgArrays;

/**
 * Invoca {@code fn_configurar_2fa} y {@code fn_desactivar_2fa} con
 * {@link NamedParameterJdbcTemplate} en vez de {@code @Query(nativeQuery = true)} (P6 de la guía
 * del examen suspenso). Ver {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate
 * que esto evita.
 */
@RequiredArgsConstructor
public class TwoFactorAuthenticationRepositoryImpl implements TwoFactorAuthenticationRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idUsuario identificador del usuario
     * @param llaveSecreta secreto TOTP a configurar
     * @param hashes hashes de los códigos de respaldo a insertar
     * @return el número de códigos de respaldo insertados
     */
    @Override
    public Integer configurar2Fa(Long idUsuario, String llaveSecreta, String[] hashes) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_usuario", idUsuario)
                .addValue("p_llave_secreta", llaveSecreta)
                .addValue("p_hashes", PgArrays.textArray(jdbcTemplate, hashes));
        return jdbcTemplate.queryForObject(
                "SELECT fn_configurar_2fa(:p_id_usuario, :p_llave_secreta, :p_hashes)", params, Integer.class);
    }

    /**
     * @param idUsuario identificador del usuario
     * @return {@code true} si tenía 2FA configurado y se desactivó; idempotente
     */
    @Override
    public Boolean desactivar2Fa(Long idUsuario) {
        MapSqlParameterSource params = new MapSqlParameterSource("p_id_usuario", idUsuario);
        return jdbcTemplate.queryForObject("SELECT fn_desactivar_2fa(:p_id_usuario)", params, Boolean.class);
    }
}
