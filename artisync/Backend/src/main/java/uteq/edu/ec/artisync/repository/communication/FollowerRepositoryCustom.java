package uteq.edu.ec.artisync.repository.communication;

/**
 * Invocaciones de {@code fn_seguir_creador}, {@code fn_dejar_de_seguir_creador},
 * {@code fn_es_seguidor} y {@code fn_conteo_seguidores} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link FollowerRepositoryImpl}).
 */
public interface FollowerRepositoryCustom {

    /** @return {@code true} si la operación de seguir se realizó */
    Boolean ejecutarFnSeguirCreador(Long idUsuario, Long idPerfil);

    /** @return {@code true} si la operación de dejar de seguir se realizó */
    Boolean ejecutarFnDejarDeSeguirCreador(Long idUsuario, Long idPerfil);

    /** @return {@code true} si el usuario sigue a ese perfil de creador */
    Boolean ejecutarFnEsSeguidor(Long idUsuario, Long idPerfil);

    /** @return la cantidad de seguidores de ese perfil */
    Long ejecutarFnConteoSeguidores(Long idPerfil);
}
