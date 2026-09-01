package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.PagedResponse;

public interface IReporteContratoServicio {

    PagedResponse<FilaReporteContrato> listar(FiltroReporteContrato filtro, int page, int size);

    DocumentoGenerado exportar(FiltroReporteContrato filtro, FormatoReporte formato, String correoSolicitante);
}
