package uteq.edu.ec.artisync.repository.social;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Invoca {@code fn_seleccionar_ganadores_sorteo} con {@link NamedParameterJdbcTemplate} en vez
 * de {@code @Query(nativeQuery = true)}: la función no cambia, solo el mecanismo Java que la
 * invoca (P6 de la guía del examen suspenso). {@code @Procedure} no es viable aquí porque su
 * retorno no-void rompe con Hibernate 7.4.x contra Postgres (ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14); {@code NamedParameterJdbcTemplate} nunca pasa por
 * ese traductor.
 */
@RequiredArgsConstructor
public class RaffleRepositoryImpl implements RaffleRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idSorteo identificador del sorteo
     * @return JSONB serializado como texto con el resultado del sorteo
     */
    @Override
    public String seleccionarGanadores(Long idSorteo) {
        MapSqlParameterSource params = new MapSqlParameterSource("p_id_sorteo", idSorteo);
        return jdbcTemplate.queryForObject(
                "SELECT fn_seleccionar_ganadores_sorteo(:p_id_sorteo)::text", params, String.class);
    }
}
