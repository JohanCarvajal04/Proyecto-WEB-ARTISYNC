package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;

import java.time.LocalDateTime;

public interface ContratoRepositoryCustom {

    /**
     * Proyección para el reporte de contratos (service/legal/impl/ReporteContratoServicioImpl).
     * "Firmado" se deriva de que el hash de firma no sea nulo — igual criterio
     * que ContratoVistaComponent.yaFirme en el frontend.
     */
    Page<FilaReporteContrato> buscarParaReporte(LocalDateTime desde, LocalDateTime hasta,
                                                 Long idPerfilCreador, Boolean soloFirmados,
                                                 Pageable pageable);
}
