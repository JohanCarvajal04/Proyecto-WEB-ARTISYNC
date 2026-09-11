package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.PagedResponse;

public interface IReporteContratoServicio {

    /**
     * Lista, paginadas, las filas del reporte de contratos que cumplen el filtro indicado.
     *
     * @param filtro criterios de filtrado (estado, rango de fechas, etc.)
     * @param page   número de página, base cero
     * @param size   tamaño de página
     * @return la página de filas que cumplen el filtro
     */
    PagedResponse<FilaReporteContrato> listar(FiltroReporteContrato filtro, int page, int size);

    /**
     * Genera un documento con las filas del reporte de contratos que cumplen el filtro indicado.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con las filas filtradas
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el filtro devuelve más filas que el tope admitido por el formato
     */
    DocumentoGenerado exportar(FiltroReporteContrato filtro, FormatoReporte formato, String correoSolicitante);

    /**
     * Genera un documento con las filas del reporte de contratos que cumplen el filtro indicado,
     * admitiendo paginación / división en partes para grandes volúmenes de datos.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param page              número de página / parte (base 0), o null para exportar sin paginación
     * @param size              tamaño de página / parte, o null para usar el tope del formato
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con las filas filtradas
     */
    DocumentoGenerado exportar(FiltroReporteContrato filtro, FormatoReporte formato, Integer page, Integer size, String correoSolicitante);
}
