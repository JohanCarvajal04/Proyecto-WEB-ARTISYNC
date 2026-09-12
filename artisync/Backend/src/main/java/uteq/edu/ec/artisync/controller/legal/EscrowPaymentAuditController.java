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
import uteq.edu.ec.artisync.dto.peticion.legal.EscrowPaymentFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowPaymentResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowPaymentDetailResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowSummaryResponse;
import uteq.edu.ec.artisync.service.legal.IEscrowPaymentAuditService;

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
public class EscrowPaymentAuditController {

    private final IEscrowPaymentAuditService pagoGarantiaAuditoriaServicio;

    /**
     * Lista los pagos en garantía (escrow) de forma paginada, con filtros opcionales.
     *
     * @param filtro criterios opcionales para filtrar el listado
     * @param pageable configuración de paginación
     * @return página con los pagos en garantía que cumplen el filtro
     */
    @Operation(summary = "Listado paginado y filtrado de pagos en garantía (escrow)")
    @GetMapping
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<EscrowPaymentResponse>> listar(EscrowPaymentFilter filtro, Pageable pageable) {
        return ResponseEntity.ok(pagoGarantiaAuditoriaServicio.listar(filtro, pageable));
    }

    /**
     * Obtiene el detalle de un pago en garantía junto con su historial de transacciones.
     *
     * @param idPago identificador del pago en garantía
     * @return el detalle del pago en garantía
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pago en garantía no existe
     */
    @Operation(summary = "Detalle de un pago en garantía, con su historial de transacciones")
    @GetMapping("/{idPago}")
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<EscrowPaymentDetailResponse> obtenerDetalle(@PathVariable Long idPago) {
        return ResponseEntity.ok(pagoGarantiaAuditoriaServicio.obtenerDetalle(idPago));
    }

    /**
     * Obtiene el resumen agregado de fondos en garantía: cantidad y monto total por estado.
     *
     * @return listado con el resumen de fondos en escrow por estado
     */
    @Operation(summary = "Resumen agregado: cantidad y monto total de fondos por estado")
    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<List<EscrowSummaryResponse>> obtenerResumen() {
        return ResponseEntity.ok(pagoGarantiaAuditoriaServicio.obtenerResumen());
    }
}
