package uteq.edu.ec.artisync.repository.seguridad;

/**
 * Proyección de Spring Data para una fila devuelta por
 * {@code fn_revocar_sesiones_usuario}: una sesión que quedó revocada.
 */
public interface RevokedSessionProjection {
    /** @return el JTI del token de la sesión revocada */
    String getJti();
    /** @return segundos que le quedaban de vigencia al token al momento de revocarlo */
    Integer getSegundosRestantes();
}

