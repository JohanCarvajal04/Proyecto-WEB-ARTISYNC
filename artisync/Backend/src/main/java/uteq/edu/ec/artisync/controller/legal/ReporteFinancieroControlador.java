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

    @Operation(summary = "Reporte de comisiones de un creador: bruto, comisión, neto y detalle de transacciones")
    @GetMapping
    @PreAuthorize("hasAuthority('TRANSACCION_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaReporteComisiones> obtener(FiltroReporteFinanciero filtro) {
        return ResponseEntity.ok(reporteFinancieroServicio.obtenerReporteComisiones(filtro));
    }

    @Operation(summary = "Exportar el reporte de comisiones en CSV, XLSX o PDF")
    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('REPORTE_FINANCIERO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(FiltroReporteFinanciero filtro, @RequestParam FormatoReporte formato,
                                            Authentication authentication) {
        DocumentoGenerado documento = reporteFinancieroServicio.exportar(filtro, formato, authentication.getName());
        return RespuestaDocumento.de(documento);
    }
}
