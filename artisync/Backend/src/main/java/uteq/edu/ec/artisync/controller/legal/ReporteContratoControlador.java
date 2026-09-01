package uteq.edu.ec.artisync.controller.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;
import uteq.edu.ec.artisync.service.legal.IReporteContratoServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.RespuestaDocumento;

/**
 * Reporte de contratos formalizados. Cierra el permiso huérfano
 * REPORTE_CONTRATO_EXPORTAR (V19__permisos_reportes.sql): sembrado junto con
 * el motor de reportes pero sin controlador hasta ahora.
 *
 * La vista usa TRANSACCION_VER (no CONTRATO_VER) porque CONTRATO_VER también
 * lo tienen CLIENTE y CREADOR sobre sus propios contratos — este endpoint es
 * un listado administrativo transversal, no la ficha de un contrato propio.
 */
@Tag(name = "Reportes — Contratos", description = "Reporte de contratos formalizados: servicio, partes, precio y estado de firma")
@RestController
@RequestMapping("/api/v1/admin/reportes/contratos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReporteContratoControlador {

    private final IReporteContratoServicio reporteContratoServicio;

    @Operation(summary = "Listado paginado y filtrado de contratos formalizados")
    @GetMapping
    @PreAuthorize("hasAuthority('TRANSACCION_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<FilaReporteContrato>> listar(
            FiltroReporteContrato filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reporteContratoServicio.listar(filtro, page, size));
    }

    @Operation(summary = "Exportar el reporte de contratos en CSV, XLSX o PDF")
    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('REPORTE_CONTRATO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(FiltroReporteContrato filtro, @RequestParam FormatoReporte formato,
                                            Authentication authentication) {
        DocumentoGenerado documento = reporteContratoServicio.exportar(filtro, formato, authentication.getName());
        return RespuestaDocumento.de(documento);
    }
}
