package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteFinanciero;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaReporteComisiones;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;

public interface IReporteFinancieroServicio {

    RespuestaReporteComisiones obtenerReporteComisiones(FiltroReporteFinanciero filtro);

    /** Lanza ExcepcionReglaNegocio si el detalle supera el tope de filas del formato pedido. */
    DocumentoGenerado exportar(FiltroReporteFinanciero filtro, FormatoReporte formato, String correoSolicitante);
}
