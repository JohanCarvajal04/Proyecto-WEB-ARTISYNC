package uteq.edu.ec.artisync.entity.respaldo;

/**
 * Entidad del modelo de dominio que representa Disparador de un respaldo (CRON automatico o MANUAL).
 * 
 * Ciclo de vida: Constante enumerada del dominio.
 * 
 * Relaciones principales: Diferencia ejecuciones agendadas de snapshots ad-hoc.
 */
public enum OrigenRespaldo {
    MANUAL,
    PROGRAMADO
}


