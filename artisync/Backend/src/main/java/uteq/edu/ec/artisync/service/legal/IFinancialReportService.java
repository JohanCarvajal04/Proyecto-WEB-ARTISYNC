package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.FinancialReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.CommissionReportResponse;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;

public interface IFinancialReportService {

    /**
     * Calcula el reporte de comisiones (bruto, comisión de plataforma y neto) que cumple el filtro indicado.
     *
     * @param filtro criterios de filtrado (rango de fechas, creador, etc.)
     * @return el reporte de comisiones agregado y su detalle
     */
    CommissionReportResponse obtenerReporteComisiones(FinancialReportFilter filtro);

    /**
     * Genera un documento con el detalle del reporte de comisiones que cumple el filtro indicado.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el detalle filtrado
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el detalle supera el tope de filas del formato pedido
     */
    GeneratedDocument exportar(FinancialReportFilter filtro, ReportFormat formato, String correoSolicitante);

    /**
     * Genera un documento con el detalle del reporte de comisiones que cumple el filtro indicado,
     * admitiendo paginación / división en partes para grandes volúmenes de datos.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param page              número de página / parte (base 0), o null para exportar sin paginación
     * @param size              tamaño de página / parte, o null para usar el tope del formato
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el detalle filtrado
     */
    GeneratedDocument exportar(FinancialReportFilter filtro, ReportFormat formato, Integer page, Integer size, String correoSolicitante);
}
