package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Types;

/**
 * Invoca {@code fn_guardar_pais} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita.
 * {@code p_id_pais} es {@code Types.BIGINT} explícito porque puede llegar {@code null} (creación) y
 * Postgres necesita el tipo declarado para inferir la sobrecarga de la función.
 */
@RequiredArgsConstructor
public class CountryRepositoryImpl implements CountryRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idPais identificador del país a renombrar, o {@code null} para crear uno nuevo
     * @param nombrePais nombre del país
     * @return el id_pais afectado
     */
    @Override
    public Long guardarPais(Long idPais, String nombrePais) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_pais", idPais, Types.BIGINT)
                .addValue("p_nombre_pais", nombrePais, Types.VARCHAR);
        return jdbcTemplate.queryForObject(
                "SELECT fn_guardar_pais(:p_id_pais, :p_nombre_pais)", params, Long.class);
    }
}
