package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.legal.ContractReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractReportRow;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.service.legal.IContractReportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnType;
import uteq.edu.ec.artisync.service.shared.reporte.ReportTotal;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cierra el permiso huérfano REPORTE_CONTRATO_EXPORTAR (V19__permisos_reportes.sql):
 * sembrado desde que se creó el motor común de reportes, pero sin controlador
 * ni servicio hasta ahora. Calcado de FinancialReportServiceImpl.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractReportServiceImpl implements IContractReportService {

    private final ContractRepository contratoRepository;
    private final IExportService servicioExportacion;

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param page parametro requerido para la correcta ejecucion del procedimiento
     * @param size parametro requerido para la correcta ejecucion del procedimiento
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PagedResponse<ContractReportRow> list(ContractReportFilter filtro, int page, int size) {
        Page<ContractReportRow> resultado = contratoRepository.buscarParaReporte(
                filtro.getDesde(), filtro.getHasta(), filtro.getIdPerfilCreador(), filtro.getSoloFirmados(),
                PageRequest.of(page, size));
        return PagedResponseBuilder.build(resultado);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "REPORTE_CONTRATO_EXPORTAR", modulo = AuditModule.FINANZAS,
            entidad = "contratos", detalle = "{formato: #formato, page: #page, size: #size}")
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param page parametro requerido para la correcta ejecucion del procedimiento
     * @param size parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public GeneratedDocument export(ContractReportFilter filtro, ReportFormat formato, Integer page, Integer size, String correoSolicitante) {
        Page<ContractReportRow> pagina;
        String titulo = "Contratos";
        String subtitulo = "Reporte de contratos formalizados";

        if (page != null) {
            int pageSize = (size != null && size > 0 && size <= formato.topeFilas()) ? size : formato.topeFilas();
            pagina = contratoRepository.buscarParaReporte(
                    filtro.getDesde(), filtro.getHasta(), filtro.getIdPerfilCreador(), filtro.getSoloFirmados(),
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "fechaFormalizacion")));
            int parte = page + 1;
            int totalPartes = Math.max(1, pagina.getTotalPages());
            titulo = "Contratos - Parte " + parte;
            subtitulo = "Reporte de contratos formalizados — Parte " + parte + " de " + totalPartes
                    + " (" + pagina.getTotalElements() + " contratos en total)";
        } else {
            pagina = contratoRepository.buscarParaReporte(
                    filtro.getDesde(), filtro.getHasta(), filtro.getIdPerfilCreador(), filtro.getSoloFirmados(),
                    PageRequest.of(0, formato.topeFilas(), Sort.by(Sort.Direction.DESC, "fechaFormalizacion")));

            if (pagina.getTotalElements() > formato.topeFilas()) {
                throw new BusinessRuleException(
                        "El reporte devuelve " + pagina.getTotalElements() + " contratos, más de los "
                                + formato.topeFilas() + " que admite una exportación en " + formato
                                + ". Acote el rango de fechas o utilice la opción de export por partes.");
            }
        }

        log.info("Reporte de contratos exportado en formato {} (page={}, size={}) por {}", formato, page, size, correoSolicitante);

        ReportModel<ContractReportRow> modelo = ReportModel.<ContractReportRow>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(filtrosLegibles(filtro))
                .columnas(List.of(
                        ReportColumn.entero("Id. contrato", ContractReportRow::idContrato),
                        ReportColumn.entero("Id. pedido", ContractReportRow::idPedido),
                        ReportColumn.texto("Offering", ContractReportRow::servicio),
                        ReportColumn.texto("Cliente", ContractReportRow::cliente),
                        ReportColumn.texto("Creador", ContractReportRow::creador),
                        ReportColumn.moneda("Precio pactado", ContractReportRow::precioPactado),
                        ReportColumn.entero("Límite de revisiones", ContractReportRow::limiteRevisiones),
                        ReportColumn.fechaHora("Formalizado", ContractReportRow::fechaFormalizacion),
                        ReportColumn.booleano("Firmado (cliente)", ContractReportRow::firmadoCliente),
                        ReportColumn.booleano("Firmado (creador)", ContractReportRow::firmadoCreador)))
                .filas(pagina.getContent())
                .totales(List.of(new ReportTotal("Importe pactado total", sumPrices(pagina.getContent()), ColumnType.MONEDA)))
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public GeneratedDocument export(ContractReportFilter filtro, ReportFormat formato, String correoSolicitante) {
        return export(filtro, formato, null, null, correoSolicitante);
    }

    private BigDecimal sumPrices(List<ContractReportRow> filas) {
        return filas.stream()
                .map(ContractReportRow::precioPactado)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, String> filtrosLegibles(ContractReportFilter filtro) {
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
