package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.ContractReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractReportRow;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.util.PagedResponse;

public interface IContractReportService {

    /**
     * Lista, paginadas, las filas del reporte de contratos que cumplen el filtro indicado.
     *
     * @param filtro criterios de filtrado (estado, rango de fechas, etc.)
     * @param page   número de página, base cero
     * @param size   tamaño de página
     * @return la página de filas que cumplen el filtro
     */
    PagedResponse<ContractReportRow> list(ContractReportFilter filtro, int page, int size);

    /**
     * Genera un documento con las filas del reporte de contratos que cumplen el filtro indicado.
     *
     * @param filtro            criterios de filtrado a export
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con las filas filtradas
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el filtro devuelve más filas que el tope admitido por el formato
     */
    GeneratedDocument export(ContractReportFilter filtro, ReportFormat formato, String correoSolicitante);

    /**
     * Genera un documento con las filas del reporte de contratos que cumplen el filtro indicado,
     * admitiendo paginación / división en partes para grandes volúmenes de datos.
     *
     * @param filtro            criterios de filtrado a export
     * @param formato           formato del documento a generar
     * @param page              número de página / parte (base 0), o null para export sin paginación
     * @param size              tamaño de página / parte, o null para usar el tope del formato
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con las filas filtradas
     */
    GeneratedDocument export(ContractReportFilter filtro, ReportFormat formato, Integer page, Integer size, String correoSolicitante);
}
