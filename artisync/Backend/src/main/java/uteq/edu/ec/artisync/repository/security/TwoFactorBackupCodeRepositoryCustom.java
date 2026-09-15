package uteq.edu.ec.artisync.repository.security;

/**
 * Invocación de {@code fn_consumir_codigo_respaldo_2fa} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link TwoFactorBackupCodeRepositoryImpl}).
 */
public interface TwoFactorBackupCodeRepositoryCustom {

    /** fn_consumir_codigo_respaldo_2fa: consume un código de respaldo una sola vez (UPDATE atómico WHERE usado=FALSE). */
    Boolean consumirCodigoRespaldo(Long idUsuario, String codigoHash);
}
