package uteq.edu.ec.artisync.audit;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Snapshot inmutable de un evento de auditoría, capturado por
 * {@link AuditAspect} en el hilo de la petición original antes de
 * persistirlo. Al ser inmutable y no depender de ThreadLocal ni de
 * SecurityContextHolder, pasar la escritura a {@code @Async} en el futuro es
 * cambiar una anotación en el servicio, no rediseñar este contrato.
 *
 * @param fechaEvento momento en que ocurrió el evento
 * @param idUsuarioActor id del usuario que realizó la acción, {@code null} si es anónimo o del sistema
 * @param correoActor correo del usuario que realizó la acción
 * @param modulo módulo de negocio afectado
 * @param accion nombre de la acción auditada
 * @param resultado resultado de la acción (éxito o fallo)
 * @param entidadAfectada tipo de entidad afectada por la acción
 * @param idEntidadAfectada id de la entidad afectada
 * @param detalleCambio detalle adicional de la acción, serializado como mapa
 * @param mensajeError mensaje de error, si {@code resultado} indica fallo
 * @param direccionIp dirección IP de origen de la petición
 * @param agenteUsuario cabecera User-Agent del cliente
 * @param metodoHttp método HTTP de la petición
 * @param rutaSolicitud ruta solicitada
 * @param duracionMs duración de la operación en milisegundos
 */
public record AuditEventData(
        LocalDateTime fechaEvento,
        Long idUsuarioActor,
        String correoActor,
        AuditModule modulo,
        String accion,
        AuditResult resultado,
        String entidadAfectada,
        Long idEntidadAfectada,
        Map<String, Object> detalleCambio,
        String mensajeError,
        String direccionIp,
        String agenteUsuario,
        String metodoHttp,
        String rutaSolicitud,
        Integer duracionMs
) {
}
