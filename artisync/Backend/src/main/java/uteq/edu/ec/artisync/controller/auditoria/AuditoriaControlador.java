package uteq.edu.ec.artisync.controller.auditoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.auditoria.FiltroAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoriaResumen;
import uteq.edu.ec.artisync.service.auditoria.IAuditoriaServicio;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bitácora inmutable de eventos del sistema (REQ-NF-013). Ver
 * V12__modulo_auditoria.sql para la garantía de inmutabilidad a nivel de
 * base de datos.
 *
 * No confundir con AuditControlador (paquete controller.social): ese
 * controlador es un exportador de transacciones de un creador concreto,
 * preexistente y sin relación con esta bitácora transversal.
 */
@Tag(name = "Auditoría", description = "Bitácora inmutable de eventos del sistema (REQ-NF-013)")
@RestController
@RequestMapping("/api/v1/admin/auditoria")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaControlador {

    private final IAuditoriaServicio auditoriaServicio;

    @Operation(summary = "Listado paginado y filtrado de la bitácora de auditoría")
    @GetMapping
    @PreAuthorize("hasAuthority('AUDITORIA_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<RespuestaEventoAuditoriaResumen>> listar(
            FiltroAuditoria filtro, Pageable pageable) {
        return ResponseEntity.ok(auditoriaServicio.listar(filtro, pageable));
    }

    @Operation(summary = "Detalle completo de un evento, incluido el JSON del cambio")
    @GetMapping("/{idEvento}")
    @PreAuthorize("hasAuthority('AUDITORIA_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaEventoAuditoria> obtenerPorId(@PathVariable Long idEvento) {
        return ResponseEntity.ok(auditoriaServicio.obtenerPorId(idEvento));
    }

    @Operation(summary = "Catálogo de acciones distintas registradas, para poblar el filtro")
    @GetMapping("/acciones")
    @PreAuthorize("hasAuthority('AUDITORIA_VER') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> listarAcciones() {
        return ResponseEntity.ok(auditoriaServicio.listarAccionesDisponibles());
    }

    @Operation(summary = "Exportar a CSV los eventos que coinciden con el filtro (tope 50 000 filas)")
    @GetMapping("/csv")
    @PreAuthorize("hasAuthority('AUDITORIA_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarCsv(FiltroAuditoria filtro) {
        byte[] csv = auditoriaServicio.exportarCsv(filtro);
        String nombreArchivo = "auditoria_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }
}
