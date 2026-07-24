package uteq.edu.ec.artisync.service.social;

/**
 * Servicio de auditoría y exportación de datos.
 * RNF-13: El historial de transacciones es inmutable y exportable en CSV.
 */
public interface AuditService {

    /**
     * Genera y retorna el contenido CSV con el historial de transacciones de un creador.
     * Solo accesible para usuarios con rol ADMIN.
     *
     * @param idCreadorPerfil ID del perfil del creador cuyos datos se exportan
     * @return bytes del archivo CSV en UTF-8
     */
    byte[] exportarTransaccionesCreadorCsv(Long idCreadorPerfil);
}
