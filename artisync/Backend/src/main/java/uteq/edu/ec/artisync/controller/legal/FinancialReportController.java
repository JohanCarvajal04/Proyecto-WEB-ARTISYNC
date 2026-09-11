package uteq.edu.ec.artisync.controller.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.FinancialReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.CommissionReportResponse;
import uteq.edu.ec.artisync.service.legal.IFinancialReportService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.util.DocumentResponse;

/**
 * Reporte financiero por creador, sobre fn_reporte_comisiones_creador
 * (db/procs/fn_reporte_comisiones_creador.sql). Ver
 * (retirado) social/AuditControlador en el historial de git: este controlador
 * lo absorbe y corrige — el CSV viejo no tenía tope de filas ni permiso
 * TRANSACCION_VER, solo hasRole('ADMIN').
 */
@Tag(name = "Reportes — Finanzas", description = "Reporte de comisiones por creador (bruto, comisión, neto y detalle)")
@RestController
@RequestMapping("/api/v1/admin/reportes/finanzas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FinancialReportController {

    private final IFinancialReportService reporteFinancieroServicio;

    /**
     * Obtiene el reporte de comisiones de un creador: monto bruto, comisión, neto y detalle de transacciones.
     *
     * @param filtro criterios para acotar el reporte (creador, rango de fechas, etc.)
     * @return el reporte de comisiones solicitado
     */
    @Operation(summary = "Reporte de comisiones de un creador: bruto, comisión, neto y detalle de transacciones")
    @GetMapping
    @PreAuthorize("hasAuthority('TRANSACCION_VER') or hasRole('ADMIN')")
    public ResponseEntity<CommissionReportResponse> obtener(FinancialReportFilter filtro) {
        return ResponseEntity.ok(reporteFinancieroServicio.obtenerReporteComisiones(filtro));
    }

    /**
     * Exporta el reporte de comisiones de un creador en el formato solicitado, con soporte de paginación/división en partes.
     *
     * @param filtro criterios para acotar el reporte (creador, rango de fechas, etc.)
     * @param formato formato del documento a generar (CSV, XLSX o PDF)
     * @param page número de página / parte solicitada (base 0, opcional)
     * @param size tamaño del lote / página (opcional)
     * @param authentication autenticación del usuario que solicita la exportación
     * @return el documento generado con el reporte de comisiones
     */
    @Operation(summary = "Exportar el reporte de comisiones en CSV, XLSX o PDF con soporte de paginación / división en partes")
    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('REPORTE_FINANCIERO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(
            FinancialReportFilter filtro,
            @RequestParam ReportFormat formato,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        GeneratedDocument documento = (page != null || size != null)
                ? reporteFinancieroServicio.exportar(filtro, formato, page, size, authentication.getName())
                : reporteFinancieroServicio.exportar(filtro, formato, authentication.getName());
        return DocumentResponse.de(documento);
    }

    /**
     * Sobrecarga de conveniencia para exportar el reporte de comisiones sin paginación (documento completo).
     *
     * @param filtro criterios para acotar el reporte (creador, rango de fechas, etc.)
     * @param formato formato del documento a generar (CSV, XLSX o PDF)
     * @param authentication autenticación del usuario actual
     * @return el documento generado con la totalidad de los registros que cumplen el filtro
     */
    @PreAuthorize("hasAuthority('REPORTE_FINANCIERO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(
            FinancialReportFilter filtro,
            ReportFormat formato,
            Authentication authentication) {
        return exportar(filtro, formato, null, null, authentication);
    }
}
