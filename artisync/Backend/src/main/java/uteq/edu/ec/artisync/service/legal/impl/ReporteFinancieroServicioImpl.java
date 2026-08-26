package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteFinanciero;
import uteq.edu.ec.artisync.dto.respuesta.legal.DetalleComision;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaReporteComisiones;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.service.legal.IReporteFinancieroServicio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.TipoColumna;
import uteq.edu.ec.artisync.service.shared.reporte.TotalReporte;

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
 * función SQL, y el detalle pasa por FormateadorValores (Locale fijo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteFinancieroServicioImpl implements IReporteFinancieroServicio {

    private static final DateTimeFormatter FORMATO_FECHA_SQL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TransaccionPagoRepository transaccionPagoRepository;
    private final IServicioExportacion servicioExportacion;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public RespuestaReporteComisiones obtenerReporteComisiones(FiltroReporteFinanciero filtro) {
        return parsear(consultar(filtro));
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "REPORTE_FINANCIERO_EXPORTAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "perfiles_creadores", idEntidad = "#filtro.idPerfil", detalle = "{formato: #formato}")
    public DocumentoGenerado exportar(FiltroReporteFinanciero filtro, FormatoReporte formato, String correoSolicitante) {
        RespuestaReporteComisiones reporte = parsear(consultar(filtro));

        if (reporte.detalle().size() > formato.topeFilas()) {
            throw new ExcepcionReglaNegocio(
                    "El reporte devuelve " + reporte.detalle().size() + " transacciones, más de las "
                            + formato.topeFilas() + " que admite una exportación en " + formato
                            + ". Acote el rango de fechas.");
        }

        ModeloReporte<DetalleComision> modelo = ModeloReporte.<DetalleComision>builder()
                .titulo("Comisiones")
                .subtitulo("Reporte financiero por creador")
                .filtrosAplicados(filtrosLegibles(filtro, reporte))
                .columnas(List.of(
                        ColumnaReporte.fechaHora("Fecha", DetalleComision::fechaEjecucion),
                        ColumnaReporte.entero("Id. transacción", DetalleComision::idTransaccion),
                        ColumnaReporte.entero("Id. pedido", DetalleComision::idPedido),
                        ColumnaReporte.texto("Servicio", DetalleComision::servicio),
                        ColumnaReporte.texto("Tipo", DetalleComision::tipo),
                        ColumnaReporte.moneda("Monto", DetalleComision::monto)))
                .filas(reporte.detalle())
                .totales(List.of(
                        new TotalReporte("Monto bruto", reporte.montoBruto(), TipoColumna.MONEDA),
                        new TotalReporte("Comisión", reporte.comision(), TipoColumna.MONEDA),
                        new TotalReporte("Monto neto", reporte.montoNeto(), TipoColumna.MONEDA)))
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    private String consultar(FiltroReporteFinanciero filtro) {
        String json = transaccionPagoRepository.reporteComisionesJson(
                filtro.getIdPerfil(), filtro.getDesde(), filtro.getHasta(), filtro.getTasaComision());
        log.info("Reporte de comisiones consultado para perfil {}", filtro.getIdPerfil());
        return json;
    }

    private RespuestaReporteComisiones parsear(String json) {
        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo interpretar el reporte de comisiones: " + e.getMessage(), e);
        }

        List<DetalleComision> detalle = new ArrayList<>();
        for (JsonNode item : nodo.get("detalle")) {
            detalle.add(new DetalleComision(
                    item.get("idTransaccion").asLong(),
                    item.get("idPedido") != null && !item.get("idPedido").isNull() ? item.get("idPedido").asLong() : null,
                    textoONulo(item.get("servicio")),
                    textoONulo(item.get("tipo")),
                    item.get("monto").decimalValue(),
                    fechaONula(item.get("fechaEjecucion"))));
        }

        return new RespuestaReporteComisiones(
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

    private Map<String, String> filtrosLegibles(FiltroReporteFinanciero filtro, RespuestaReporteComisiones reporte) {
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
