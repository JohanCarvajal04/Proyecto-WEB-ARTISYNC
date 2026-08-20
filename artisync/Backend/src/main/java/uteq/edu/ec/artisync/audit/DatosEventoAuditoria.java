package uteq.edu.ec.artisync.audit;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Snapshot inmutable de un evento de auditoría, capturado por
 * {@link AspectoAuditoria} en el hilo de la petición original antes de
 * persistirlo. Al ser inmutable y no depender de ThreadLocal ni de
 * SecurityContextHolder, pasar la escritura a {@code @Async} en el futuro es
 * cambiar una anotación en el servicio, no rediseñar este contrato.
 */
public record DatosEventoAuditoria(
        LocalDateTime fechaEvento,
        Long idUsuarioActor,
        String correoActor,
        ModuloAuditoria modulo,
        String accion,
        ResultadoAuditoria resultado,
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
