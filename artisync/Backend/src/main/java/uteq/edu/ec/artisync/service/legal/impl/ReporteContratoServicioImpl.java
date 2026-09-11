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
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param page parametro requerido para la correcta ejecucion del procedimiento
     * @param size parametro requerido para la correcta ejecucion del procedimiento
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PagedResponse<FilaReporteContrato> listar(FiltroReporteContrato filtro, int page, int size) {
        Page<FilaReporteContrato> resultado = contratoRepository.buscarParaReporte(
                filtro.getDesde(), filtro.getHasta(), filtro.getIdPerfilCreador(), filtro.getSoloFirmados(),
                PageRequest.of(page, size));
        return PagedResponseBuilder.build(resultado);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "REPORTE_CONTRATO_EXPORTAR", modulo = ModuloAuditoria.FINANZAS,
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public DocumentoGenerado exportar(FiltroReporteContrato filtro, FormatoReporte formato, Integer page, Integer size, String correoSolicitante) {
        Page<FilaReporteContrato> pagina;
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
                throw new ExcepcionReglaNegocio(
                        "El reporte devuelve " + pagina.getTotalElements() + " contratos, más de los "
                                + formato.topeFilas() + " que admite una exportación en " + formato
                                + ". Acote el rango de fechas o utilice la opción de exportar por partes.");
            }
        }

        log.info("Reporte de contratos exportado en formato {} (page={}, size={}) por {}", formato, page, size, correoSolicitante);

        ModeloReporte<FilaReporteContrato> modelo = ModeloReporte.<FilaReporteContrato>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
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

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public DocumentoGenerado exportar(FiltroReporteContrato filtro, FormatoReporte formato, String correoSolicitante) {
        return exportar(filtro, formato, null, null, correoSolicitante);
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
