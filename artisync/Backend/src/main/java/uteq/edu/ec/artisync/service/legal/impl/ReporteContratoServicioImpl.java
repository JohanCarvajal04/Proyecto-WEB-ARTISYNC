package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.service.legal.IReporteContratoServicio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.TipoColumna;
import uteq.edu.ec.artisync.service.shared.reporte.TotalReporte;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cierra el permiso huérfano REPORTE_CONTRATO_EXPORTAR (V19__permisos_reportes.sql):
 * sembrado desde que se creó el motor común de reportes, pero sin controlador
 * ni servicio hasta ahora. Calcado de ReporteFinancieroServicioImpl.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteContratoServicioImpl implements IReporteContratoServicio {

    private final ContratoRepository contratoRepository;
    private final IServicioExportacion servicioExportacion;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FilaReporteContrato> listar(FiltroReporteContrato filtro, int page, int size) {
        Page<FilaReporteContrato> resultado = contratoRepository.buscarParaReporte(
                filtro.getDesde(), filtro.getHasta(), filtro.getIdPerfilCreador(), filtro.getSoloFirmados(),
                PageRequest.of(page, size));
        return PagedResponseBuilder.build(resultado);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "REPORTE_CONTRATO_EXPORTAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "contratos", detalle = "{formato: #formato}")
    public DocumentoGenerado exportar(FiltroReporteContrato filtro, FormatoReporte formato, String correoSolicitante) {
        Page<FilaReporteContrato> pagina = contratoRepository.buscarParaReporte(
                filtro.getDesde(), filtro.getHasta(), filtro.getIdPerfilCreador(), filtro.getSoloFirmados(),
                PageRequest.of(0, formato.topeFilas(), Sort.by(Sort.Direction.DESC, "fechaFormalizacion")));

        if (pagina.getTotalElements() > formato.topeFilas()) {
            throw new ExcepcionReglaNegocio(
                    "El reporte devuelve " + pagina.getTotalElements() + " contratos, más de los "
                            + formato.topeFilas() + " que admite una exportación en " + formato
                            + ". Acote el rango de fechas.");
        }

        log.info("Reporte de contratos exportado en formato {} por {}", formato, correoSolicitante);

        ModeloReporte<FilaReporteContrato> modelo = ModeloReporte.<FilaReporteContrato>builder()
                .titulo("Contratos")
                .subtitulo("Reporte de contratos formalizados")
                .filtrosAplicados(filtrosLegibles(filtro))
                .columnas(List.of(
                        ColumnaReporte.entero("Id. contrato", FilaReporteContrato::idContrato),
                        ColumnaReporte.entero("Id. pedido", FilaReporteContrato::idPedido),
                        ColumnaReporte.texto("Servicio", FilaReporteContrato::servicio),
                        ColumnaReporte.texto("Cliente", FilaReporteContrato::cliente),
                        ColumnaReporte.texto("Creador", FilaReporteContrato::creador),
                        ColumnaReporte.moneda("Precio pactado", FilaReporteContrato::precioPactado),
                        ColumnaReporte.entero("Límite de revisiones", FilaReporteContrato::limiteRevisiones),
                        ColumnaReporte.fechaHora("Formalizado", FilaReporteContrato::fechaFormalizacion),
                        ColumnaReporte.booleano("Firmado (cliente)", FilaReporteContrato::firmadoCliente),
                        ColumnaReporte.booleano("Firmado (creador)", FilaReporteContrato::firmadoCreador)))
                .filas(pagina.getContent())
                .totales(List.of(new TotalReporte("Importe pactado total", sumarPrecios(pagina.getContent()), TipoColumna.MONEDA)))
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    private BigDecimal sumarPrecios(List<FilaReporteContrato> filas) {
        return filas.stream()
                .map(FilaReporteContrato::precioPactado)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, String> filtrosLegibles(FiltroReporteContrato filtro) {
        Map<String, String> filtros = new LinkedHashMap<>();
        if (filtro.getDesde() != null) {
            filtros.put("Desde", filtro.getDesde().toString());
        }
        if (filtro.getHasta() != null) {
            filtros.put("Hasta", filtro.getHasta().toString());
        }
        if (filtro.getIdPerfilCreador() != null) {
            filtros.put("Id. perfil del creador", String.valueOf(filtro.getIdPerfilCreador()));
        }
        if (filtro.getSoloFirmados() != null) {
            filtros.put("Solo firmados", filtro.getSoloFirmados() ? "Sí" : "No");
        }
        return filtros;
    }
}
