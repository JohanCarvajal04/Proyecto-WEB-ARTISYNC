package uteq.edu.ec.artisync.controller.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantiaDetalle;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow;
import uteq.edu.ec.artisync.service.legal.IPagoGarantiaAuditoriaServicio;

import java.util.List;

/**
 * Supervisión de Pagos y Garantías (Escrow), gateada en PAGO_AUDITAR: el
 * permiso ya estaba asignado a AUDITOR_FINANCIERO desde el seed inicial
 * (V1__schema_inicial.sql) pero ningún endpoint lo comprobaba todavía.
 */
@Tag(name = "Admin — Pagos y Garantías", description = "Supervisión de fondos en escrow para el Auditor Financiero")
@RestController
@RequestMapping("/api/v1/admin/pagos-garantia")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PagoGarantiaAuditoriaControlador {

    private final IPagoGarantiaAuditoriaServicio pagoGarantiaAuditoriaServicio;

    @Operation(summary = "Listado paginado y filtrado de pagos en garantía (escrow)")
    @GetMapping
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<RespuestaPagoGarantia>> listar(FiltroPagoGarantia filtro, Pageable pageable) {
        return ResponseEntity.ok(pagoGarantiaAuditoriaServicio.listar(filtro, pageable));
    }

    @Operation(summary = "Detalle de un pago en garantía, con su historial de transacciones")
    @GetMapping("/{idPago}")
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaPagoGarantiaDetalle> obtenerDetalle(@PathVariable Long idPago) {
        return ResponseEntity.ok(pagoGarantiaAuditoriaServicio.obtenerDetalle(idPago));
    }

    @Operation(summary = "Resumen agregado: cantidad y monto total de fondos por estado")
    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaResumenEscrow>> obtenerResumen() {
        return ResponseEntity.ok(pagoGarantiaAuditoriaServicio.obtenerResumen());
    }
}
