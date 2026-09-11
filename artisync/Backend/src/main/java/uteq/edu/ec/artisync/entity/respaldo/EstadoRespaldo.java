package uteq.edu.ec.artisync.entity.respaldo;

/**
 * Entidad del modelo de dominio que representa Ciclo de vida de un respaldo de base de datos.
 * 
 * Ciclo de vida: Constante enumerada que refleja transiciones (EN_PROGRESO, COMPLETADO, FALLIDO).
 * 
 * Relaciones principales: Anotada sobre registros de auditoria administrativa.
 */
public enum EstadoRespaldo {
    EN_PROGRESO,
    COMPLETADO,
    FALLIDO
}


