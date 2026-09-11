package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteFinanciero;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaReporteComisiones;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;

public interface IReporteFinancieroServicio {

    /**
     * Calcula el reporte de comisiones (bruto, comisión de plataforma y neto) que cumple el filtro indicado.
     *
     * @param filtro criterios de filtrado (rango de fechas, creador, etc.)
     * @return el reporte de comisiones agregado y su detalle
     */
    RespuestaReporteComisiones obtenerReporteComisiones(FiltroReporteFinanciero filtro);

    /**
     * Genera un documento con el detalle del reporte de comisiones que cumple el filtro indicado.
     *
     * @param filtro            criterios de filtrado a exportar
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el detalle filtrado
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el detalle supera el tope de filas del formato pedido
     */
    DocumentoGenerado exportar(FiltroReporteFinanciero filtro, FormatoReporte formato, String correoSolicitante);

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
    DocumentoGenerado exportar(FiltroReporteFinanciero filtro, FormatoReporte formato, Integer page, Integer size, String correoSolicitante);
}
