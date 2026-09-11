package uteq.edu.ec.artisync.controller.legal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uteq.edu.ec.artisync.dto.respuesta.legal.IntegrityVerificationResponse;
import uteq.edu.ec.artisync.service.legal.IContractService;

/**
 * Verificación de integridad de contratos firmados (REQ-NF-020), gateada en
 * PAGO_AUDITAR: mismo dominio de supervisión financiera/legal que ya
 * gobierna ese permiso (ver EscrowPaymentAuditController) — no se crea
 * un permiso nuevo para esto.
 */
@Tag(name = "Admin — Integridad de contratos", description = "Re-verificación del hash de contenido de contratos firmados")
@RestController
@RequestMapping("/api/v1/admin/contratos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ContractIntegrityController {

    private final IContractService contratoServicio;

    /**
     * Recalcula el hash de contenido de un contrato y lo compara con el
     * guardado al momento de la firma.
     *
     * @param idContrato id del contrato a verificar
     * @return el resultado de la comparación
     */
    @Operation(summary = "Verificar la integridad de un contrato firmado")
    @PostMapping("/{idContrato}/verificar-integridad")
    @PreAuthorize("hasAuthority('PAGO_AUDITAR') or hasRole('ADMIN')")
    public ResponseEntity<IntegrityVerificationResponse> verificarIntegridad(@PathVariable Long idContrato) {
        return ResponseEntity.ok(contratoServicio.verificarIntegridadHash(idContrato));
    }
}
