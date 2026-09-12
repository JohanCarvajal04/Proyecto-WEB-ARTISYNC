package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.legal.FinancialReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.CommissionDetail;
import uteq.edu.ec.artisync.dto.respuesta.legal.CommissionReportResponse;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.legal.IFinancialReportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnType;
import uteq.edu.ec.artisync.service.shared.reporte.ReportTotal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Activa fn_reporte_comisiones_creador (db/procs/fn_reporte_comisiones_creador.sql),
 * que estaba escrita, documentada y verificada por CI sin ningún llamador en
 * Java. Sustituye al viejo AuditServiceImpl.exportarTransaccionesCreadorCsv
 * (retirado): ese exportador no tenía tope de filas, no llevaba BOM UTF-8 y
 * formateaba el monto con String.format("%.2f", ...), que hereda el locale por
 * defecto de la JVM y en es-ES produce coma decimal — partiendo la columna de
 * un CSV separado por comas. Aquí el bruto/comisión/neto los calcula la
 * función SQL, y el detalle pasa por ValueFormatter (Locale fijo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements IFinancialReportService {

    private static final DateTimeFormatter FORMATO_FECHA_SQL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PaymentTransactionRepository transaccionPagoRepository;
    private final IExportService servicioExportacion;
    private final ObjectMapper objectMapper;

    /**
     * Fuente unica con DeliverableServiceImpl.tasaComision: si el filtro no
     * trae una tasa explicita, se usa esta en vez de dejar que
     * fn_reporte_comisiones_creador aplique su propio default 0.1000
     * independiente en SQL.
     */
    @Value("${plataforma.comision-tasa:0.10}")
    private BigDecimal tasaComisionPorDefecto;

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public CommissionReportResponse getCommissionReport(FinancialReportFilter filtro) {
        return parsear(query(filtro));
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "REPORTE_FINANCIERO_EXPORTAR", modulo = AuditModule.FINANZAS,
            entidad = "perfiles_creadores", idEntidad = "#filtro.idPerfil", detalle = "{formato: #formato, page: #page, size: #size}")
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
    public GeneratedDocument export(FinancialReportFilter filtro, ReportFormat formato, Integer page, Integer size, String correoSolicitante) {
        CommissionReportResponse reporte = parsear(query(filtro));
        List<CommissionDetail> filas;
        String titulo = "Comisiones";
        String subtitulo = "Reporte financiero por creador";

        if (page != null) {
            int pageSize = (size != null && size > 0 && size <= formato.topeFilas()) ? size : formato.topeFilas();
            int totalFilas = reporte.detalle().size();
            int totalPartes = Math.max(1, (int) Math.ceil((double) totalFilas / pageSize));
            int fromIndex = Math.min(page * pageSize, totalFilas);
            int toIndex = Math.min(fromIndex + pageSize, totalFilas);
            filas = reporte.detalle().subList(fromIndex, toIndex);
            int parte = page + 1;
            titulo = "Comisiones - Parte " + parte;
            subtitulo = "Reporte financiero por creador — Parte " + parte + " de " + totalPartes
                    + " (" + totalFilas + " transacciones en total)";
        } else {
            filas = reporte.detalle();
            if (filas.size() > formato.topeFilas()) {
                throw new BusinessRuleException(
                        "El reporte devuelve " + filas.size() + " transacciones, más de las "
                                + formato.topeFilas() + " que admite una exportación en " + formato
                                + ". Acote el rango de fechas o utilice la opción de export por partes.");
            }
        }

        ReportModel<CommissionDetail> modelo = ReportModel.<CommissionDetail>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(filtrosLegibles(filtro, reporte))
                .columnas(List.of(
                        ReportColumn.fechaHora("Fecha", CommissionDetail::fechaEjecucion),
                        ReportColumn.entero("Id. transacción", CommissionDetail::idTransaccion),
                        ReportColumn.entero("Id. pedido", CommissionDetail::idPedido),
                        ReportColumn.texto("Offering", CommissionDetail::servicio),
                        ReportColumn.texto("Tipo", CommissionDetail::tipo),
                        ReportColumn.moneda("Monto", CommissionDetail::monto)))
                .filas(filas)
                .totales(List.of(
                        new ReportTotal("Monto bruto", reporte.montoBruto(), ColumnType.MONEDA),
                        new ReportTotal("Comisión", reporte.comision(), ColumnType.MONEDA),
                        new ReportTotal("Monto neto", reporte.montoNeto(), ColumnType.MONEDA)))
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
    public GeneratedDocument export(FinancialReportFilter filtro, ReportFormat formato, String correoSolicitante) {
        return export(filtro, formato, null, null, correoSolicitante);
    }

    private String query(FinancialReportFilter filtro) {
        BigDecimal tasa = filtro.getTasaComision() != null ? filtro.getTasaComision() : tasaComisionPorDefecto;
        String json = transaccionPagoRepository.reporteComisionesJson(
                filtro.getIdPerfil(), filtro.getDesde(), filtro.getHasta(), tasa);
        log.info("Reporte de comisiones consultado para perfil {}", filtro.getIdPerfil());
        return json;
    }

    private CommissionReportResponse parsear(String json) {
        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo interpretar el reporte de comisiones: " + e.getMessage(), e);
        }

        List<CommissionDetail> detalle = new ArrayList<>();
        for (JsonNode item : nodo.get("detalle")) {
            detalle.add(new CommissionDetail(
                    item.get("idTransaccion").asLong(),
                    item.get("idPedido") != null && !item.get("idPedido").isNull() ? item.get("idPedido").asLong() : null,
                    textoONulo(item.get("servicio")),
                    textoONulo(item.get("tipo")),
                    item.get("monto").decimalValue(),
                    fechaONula(item.get("fechaEjecucion"))));
        }

        return new CommissionReportResponse(
                nodo.get("idPerfil").asLong(),
                fechaONula(nodo.get("fechaDesde")),
                fechaONula(nodo.get("fechaHasta")),
                nodo.get("tasaComision").decimalValue(),
                nodo.get("totalPedidos").asLong(),
                nodo.get("totalOperaciones").asLong(),
                nodo.get("montoBruto").decimalValue(),
                nodo.get("comision").decimalValue(),
                nodo.get("montoNeto").decimalValue(),
                detalle);
    }

    private String textoONulo(JsonNode nodo) {
        return nodo == null || nodo.isNull() ? null : nodo.asText();
    }

    private LocalDateTime fechaONula(JsonNode nodo) {
        if (nodo == null || nodo.isNull()) {
            return null;
        }
        return LocalDateTime.parse(nodo.asText(), FORMATO_FECHA_SQL);
    }

    private Map<String, String> filtrosLegibles(FinancialReportFilter filtro, CommissionReportResponse reporte) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("Id. perfil", String.valueOf(filtro.getIdPerfil()));
        if (filtro.getDesde() != null) {
            filtros.put("Desde", filtro.getDesde().toString());
        }
        if (filtro.getHasta() != null) {
            filtros.put("Hasta", filtro.getHasta().toString());
        }
        filtros.put("Tasa de comisión", reporte.tasaComision().toString());
        return filtros;
    }
}
