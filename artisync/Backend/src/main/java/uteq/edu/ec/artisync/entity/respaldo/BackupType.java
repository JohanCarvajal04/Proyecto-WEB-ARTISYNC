package uteq.edu.ec.artisync.entity.respaldo;

/**
 * Entidad del modelo de dominio que representa Alcance del respaldo (completo, parcial).
 * 
 * Ciclo de vida: Constante enumerada del dominio.
 * 
 * Relaciones principales: Clasifica el tamaño y objetivo del backup de persistencia.
 */
public enum BackupType {
    FULL,
    INCREMENTAL
}


