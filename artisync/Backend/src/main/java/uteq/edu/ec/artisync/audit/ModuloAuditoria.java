package uteq.edu.ec.artisync.audit;

/**
 * Los 8 módulos de negocio, replicados aquí para que coincidan con el CHECK
 * ck_auditoria_modulo de auditoria_eventos (V12__modulo_auditoria.sql). No se
 * referencian por FK a permisos.modulo_aplicacion a propósito: ese catálogo es
 * editable por el administrador, y la bitácora no debe acoplarse a él.
 */
public enum ModuloAuditoria {
    SEGURIDAD,
    SISTEMA,
    PORTAFOLIO,
    CATALOGO,
    PEDIDOS,
    FINANZAS,
    COMUNICACION,
    SOCIAL
}
