package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Invoca {@code fn_consumir_codigo_respaldo_2fa} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita.
 */
@RequiredArgsConstructor
public class TwoFactorBackupCodeRepositoryImpl implements TwoFactorBackupCodeRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idUsuario identificador del usuario
     * @param codigoHash hash del código de respaldo presentado
     * @return {@code true} solo para el primer llamante que consume ese código
     */
    @Override
    public Boolean consumirCodigoRespaldo(Long idUsuario, String codigoHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_usuario", idUsuario)
                .addValue("p_codigo_hash", codigoHash);
        return jdbcTemplate.queryForObject(
                "SELECT fn_consumir_codigo_respaldo_2fa(:p_id_usuario, :p_codigo_hash)", params, Boolean.class);
    }
}
