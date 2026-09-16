package uteq.edu.ec.artisync.repository.security;

/**
 * Invocaciones de {@code fn_configurar_2fa} y {@code fn_desactivar_2fa} vía JDBC directo, fuera
 * del mecanismo de {@code @Query} de Spring Data JPA (ver {@link TwoFactorAuthenticationRepositoryImpl}).
 */
public interface TwoFactorAuthenticationRepositoryCustom {

    /**
     * fn_configurar_2fa: upsert atómico del secreto TOTP + códigos de respaldo. Devuelve cuántos códigos se insertaron.
     * @param idUsuario el identificador de usuario
     * @param llaveSecreta el llave secreta
     * @param hashes los hashes
     * @return el valor numerico calculado
     */
    Integer configurar2Fa(Long idUsuario, String llaveSecreta, String[] hashes);

    /**
     * fn_desactivar_2fa: desactiva 2FA y purga códigos de respaldo; idempotente.
     * @param idUsuario el identificador de usuario
     * @return true o false segun el resultado de la operacion
     */
    Boolean desactivar2Fa(Long idUsuario);
}
