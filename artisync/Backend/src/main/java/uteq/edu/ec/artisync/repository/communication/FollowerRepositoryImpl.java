package uteq.edu.ec.artisync.repository.communication;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Invoca las 4 funciones de seguidores con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita.
 */
@RequiredArgsConstructor
public class FollowerRepositoryImpl implements FollowerRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @param idUsuario identificador del usuario que sigue
     * @param idPerfil identificador del perfil de creador a seguir
     * @return {@code true} si la operación se realizó
     */
    @Override
    public Boolean ejecutarFnSeguirCreador(Long idUsuario, Long idPerfil) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idUsuario", idUsuario)
                .addValue("idPerfil", idPerfil);
        return jdbcTemplate.queryForObject("SELECT fn_seguir_creador(:idUsuario, :idPerfil)", params, Boolean.class);
    }

    /**
     * @param idUsuario identificador del usuario que deja de seguir
     * @param idPerfil identificador del perfil de creador
     * @return {@code true} si la operación se realizó
     */
    @Override
    public Boolean ejecutarFnDejarDeSeguirCreador(Long idUsuario, Long idPerfil) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idUsuario", idUsuario)
                .addValue("idPerfil", idPerfil);
        return jdbcTemplate.queryForObject(
                "SELECT fn_dejar_de_seguir_creador(:idUsuario, :idPerfil)", params, Boolean.class);
    }

    /**
     * @param idUsuario identificador del usuario
     * @param idPerfil identificador del perfil de creador
     * @return {@code true} si el usuario sigue a ese perfil de creador
     */
    @Override
    public Boolean ejecutarFnEsSeguidor(Long idUsuario, Long idPerfil) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idUsuario", idUsuario)
                .addValue("idPerfil", idPerfil);
        return jdbcTemplate.queryForObject("SELECT fn_es_seguidor(:idUsuario, :idPerfil)", params, Boolean.class);
    }

    /**
     * @param idPerfil identificador del perfil de creador
     * @return la cantidad de seguidores de ese perfil
     */
    @Override
    public Long ejecutarFnConteoSeguidores(Long idPerfil) {
        MapSqlParameterSource params = new MapSqlParameterSource("idPerfil", idPerfil);
        return jdbcTemplate.queryForObject("SELECT fn_conteo_seguidores(:idPerfil)", params, Long.class);
    }
}
