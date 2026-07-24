package uteq.edu.ec.artisync.controller.social;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.service.social.AuditService;

/**
 * Controlador de auditoría y exportación de datos.
 * RNF-13: El historial es inmutable; solo lectura. Solo accesible para ADMIN.
 * El endpoint de exportación CSV permite auditoría externa de transacciones.
 */
@Tag(name = "Auditoría", description = "Exportación de historial de transacciones para ADMIN (RNF-13)")
@RestController
@RequiredArgsConstructor
public class AuditControlador {

    private final AuditService auditService;

    @Operation(summary = "Exportar transacciones de un creador en CSV (solo ADMIN)")
    @GetMapping("/api/v1/admin/transacciones/{idPerfil}/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarCsv(@PathVariable Long idPerfil) {
        byte[] csv = auditService.exportarTransaccionesCreadorCsv(idPerfil);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transacciones_creador_" + idPerfil + ".csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }
}
