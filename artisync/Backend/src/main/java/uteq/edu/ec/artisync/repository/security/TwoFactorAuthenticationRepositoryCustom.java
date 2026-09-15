package uteq.edu.ec.artisync.repository.security;

/**
 * Invocaciones de {@code fn_configurar_2fa} y {@code fn_desactivar_2fa} vía JDBC directo, fuera
 * del mecanismo de {@code @Query} de Spring Data JPA (ver {@link TwoFactorAuthenticationRepositoryImpl}).
 */
public interface TwoFactorAuthenticationRepositoryCustom {

    /** fn_configurar_2fa: upsert atómico del secreto TOTP + códigos de respaldo. Devuelve cuántos códigos se insertaron. */
    Integer configurar2Fa(Long idUsuario, String llaveSecreta, String[] hashes);

    /** fn_desactivar_2fa: desactiva 2FA y purga códigos de respaldo; idempotente. */
    Boolean desactivar2Fa(Long idUsuario);
}
