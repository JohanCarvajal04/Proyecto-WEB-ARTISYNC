package uteq.edu.ec.artisync.controller.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.ContractReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractReportRow;
import uteq.edu.ec.artisync.service.legal.IContractReportService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.DocumentResponse;

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
public class ContractReportController {

    private final IContractReportService reporteContratoServicio;

    /**
     * Lista de forma paginada los contratos formalizados, con filtros opcionales.
     *
     * @param filtro criterios opcionales para filtrar el reporte
     * @param page número de página solicitada (base 0)
     * @param size tamaño de la página
     * @return página con las filas del reporte de contratos
     */
    @Operation(summary = "Listado paginado y filtrado de contratos formalizados")
    @GetMapping
    @PreAuthorize("hasAuthority('TRANSACCION_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ContractReportRow>> list(
            ContractReportFilter filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reporteContratoServicio.list(filtro, page, size));
    }

    /**
     * Exporta el reporte de contratos formalizados en el formato solicitado, con soporte de paginación/división en partes.
     *
     * @param filtro criterios opcionales para filtrar el reporte
     * @param formato formato del documento a generar (CSV, XLSX o PDF)
     * @param page número de página / parte solicitada (base 0, opcional)
     * @param size tamaño del lote / página (opcional)
     * @param authentication autenticación del usuario que solicita la exportación
     * @return el documento generado con el reporte de contratos
     */
    @Operation(summary = "Exportar el reporte de contratos en CSV, XLSX o PDF con soporte de paginación / división por partes")
    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('REPORTE_CONTRATO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(
            ContractReportFilter filtro,
            @RequestParam ReportFormat formato,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        GeneratedDocument documento = (page != null || size != null)
                ? reporteContratoServicio.export(filtro, formato, page, size, authentication.getName())
                : reporteContratoServicio.export(filtro, formato, authentication.getName());
        return DocumentResponse.de(documento);
    }

    /**
     * Sobrecarga de conveniencia para export el reporte de contratos sin paginación (documento completo).
     *
     * @param filtro criterios opcionales para filtrar el reporte
     * @param formato formato del documento a generar (CSV, XLSX o PDF)
     * @param authentication autenticación del usuario actual
     * @return el documento generado con la totalidad de los contratos que cumplen el filtro
     */
    @PreAuthorize("hasAuthority('REPORTE_CONTRATO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(
            ContractReportFilter filtro,
            ReportFormat formato,
            Authentication authentication) {
        return export(filtro, formato, null, null, authentication);
    }
}
