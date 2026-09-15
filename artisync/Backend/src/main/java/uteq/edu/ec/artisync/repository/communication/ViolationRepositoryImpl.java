package uteq.edu.ec.artisync.repository.communication;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Invoca {@code fn_registrar_infraccion} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita.
 */
@RequiredArgsConstructor
public class ViolationRepositoryImpl implements ViolationRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idUsuario identificador del usuario infractor
     * @param idPedido identificador del pedido donde ocurrió la infracción
     * @param mensajeOriginal texto original del mensaje detectado
     * @param patronDetectado patrón que disparó la detección
     * @return JSONB serializado como texto con el resultado del registro
     */
    @Override
    public String registerViolation(Long idUsuario, Long idPedido, String mensajeOriginal, String patronDetectado) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_usuario", idUsuario)
                .addValue("p_id_pedido", idPedido)
                .addValue("p_mensaje_original", mensajeOriginal)
                .addValue("p_patron_detectado", patronDetectado);
        return jdbcTemplate.queryForObject(
                "SELECT fn_registrar_infraccion(:p_id_usuario, :p_id_pedido, :p_mensaje_original, :p_patron_detectado)::text",
                params, String.class);
    }
}
