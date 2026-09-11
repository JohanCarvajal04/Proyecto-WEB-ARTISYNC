package uteq.edu.ec.artisync.service.auditoria;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.audit.AuditEventData;
import uteq.edu.ec.artisync.dto.peticion.auditoria.AuditFilter;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.AuditEventResponse;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.AuditEventSummaryResponse;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

public interface IAuditService {

    /**
     * Registra un evento de auditoría. Se ejecuta en una transacción nueva
     * (REQUIRES_NEW) para que el registro persista aunque la transacción que
     * lo originó termine haciendo rollback; ver {@code AuditServiceImpl}
     * para el razonamiento completo.
     *
     * @param datos datos del evento a registrar
     */
    void registrar(AuditEventData datos);

    /**
     * Lista los eventos de auditoría que cumplen el filtro indicado, paginados.
     *
     * @param filtro   criterios de filtrado (acción, usuario, rango de fechas, etc.)
     * @param pageable configuración de paginación y orden
     * @return la página de eventos que cumplen el filtro
     */
    PagedResponse<AuditEventSummaryResponse> listar(AuditFilter filtro, Pageable pageable);

    /**
     * Obtiene el detalle de un evento de auditoría por su id.
     *
     * @param idEvento id del evento
     * @return el detalle del evento
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el evento no existe
     */
    AuditEventResponse obtenerPorId(Long idEvento);

    /**
     * Genera un documento con los eventos de auditoría que cumplen el filtro indicado.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con los eventos filtrados
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el filtro devuelve más filas que el tope admitido por el formato
     */
    GeneratedDocument exportar(AuditFilter filtro, ReportFormat formato, String correoSolicitante);

    /**
     * Genera un documento con los eventos de auditoría que cumplen el filtro indicado,
     * admitiendo paginación / división en partes para grandes volúmenes de datos.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param page              número de página / parte (base 0), o null para exportar sin paginación
     * @param size              tamaño de página / parte, o null para usar el tope del formato
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con los eventos filtrados
     */
    GeneratedDocument exportar(AuditFilter filtro, ReportFormat formato, Integer page, Integer size, String correoSolicitante);

    /**
     * Lista los nombres de las acciones de auditoría ya registradas, distintos, para poblar filtros.
     *
     * @return los nombres de acción disponibles
     */
    List<String> listarAccionesDisponibles();
}
