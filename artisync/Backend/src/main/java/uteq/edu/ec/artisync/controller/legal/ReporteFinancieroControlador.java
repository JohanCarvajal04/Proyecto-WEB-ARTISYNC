package uteq.edu.ec.artisync.controller.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteFinanciero;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaReporteComisiones;
import uteq.edu.ec.artisync.service.legal.IReporteFinancieroServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.RespuestaDocumento;

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
public class ReporteFinancieroControlador {

    private final IReporteFinancieroServicio reporteFinancieroServicio;

    /**
     * Obtiene el reporte de comisiones de un creador: monto bruto, comisión, neto y detalle de transacciones.
     *
     * @param filtro criterios para acotar el reporte (creador, rango de fechas, etc.)
     * @return el reporte de comisiones solicitado
     */
    @Operation(summary = "Reporte de comisiones de un creador: bruto, comisión, neto y detalle de transacciones")
    @GetMapping
    @PreAuthorize("hasAuthority('TRANSACCION_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaReporteComisiones> obtener(FiltroReporteFinanciero filtro) {
        return ResponseEntity.ok(reporteFinancieroServicio.obtenerReporteComisiones(filtro));
    }

    /**
     * Exporta el reporte de comisiones de un creador en el formato solicitado.
     *
     * @param filtro criterios para acotar el reporte (creador, rango de fechas, etc.)
     * @param formato formato del documento a generar (CSV, XLSX o PDF)
     * @param authentication autenticación del usuario que solicita la exportación
     * @return el documento generado con el reporte de comisiones
     * @throws ExcepcionReglaNegocio si el número de transacciones del reporte excede el tope de filas admitido por el formato
     */
    @Operation(summary = "Exportar el reporte de comisiones en CSV, XLSX o PDF")
    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('REPORTE_FINANCIERO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(FiltroReporteFinanciero filtro, @RequestParam FormatoReporte formato,
                                            Authentication authentication) {
        DocumentoGenerado documento = reporteFinancieroServicio.exportar(filtro, formato, authentication.getName());
        return RespuestaDocumento.de(documento);
    }
}
