package uteq.edu.ec.artisync.repository.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invoca {@code fn_reporte_comisiones_creador} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita.
 */
@RequiredArgsConstructor
public class PaymentTransactionRepositoryImpl implements PaymentTransactionRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idPerfil identificador del perfil de creador
     * @param desde fecha inicial del rango del reporte
     * @param hasta fecha final del rango del reporte
     * @param tasa tasa de comisión de la plataforma aplicada
     * @return JSONB serializado como texto con el reporte agregado
     */
    @Override
    public String reporteComisionesJson(Long idPerfil, LocalDateTime desde, LocalDateTime hasta, BigDecimal tasa) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idPerfil", idPerfil)
                .addValue("desde", desde)
                .addValue("hasta", hasta)
                .addValue("tasa", tasa);
        return jdbcTemplate.queryForObject(
                "SELECT fn_reporte_comisiones_creador(:idPerfil, :desde, :hasta, :tasa)::text",
                params, String.class);
    }
}
