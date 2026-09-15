package uteq.edu.ec.artisync.repository.security;

import java.util.List;

/**
 * Invocación de {@code fn_revocar_sesiones_usuario} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link UserSessionRepositoryImpl}).
 */
public interface UserSessionRepositoryCustom {

    /** fn_revocar_sesiones_usuario: DELETE ... RETURNING atómico; borra y devuelve las sesiones revocadas en una sola sentencia. */
    List<RevokedSessionProjection> revocarSesionesUsuario(Long idUsuario);
}
