package uteq.edu.ec.artisync.audit;

/**
 * Debe coincidir con el CHECK ck_auditoria_resultado de auditoria_eventos
 * (V12__modulo_auditoria.sql).
 */
public enum ResultadoAuditoria {
    EXITO,
    FALLIDO,
    DENEGADO
}
