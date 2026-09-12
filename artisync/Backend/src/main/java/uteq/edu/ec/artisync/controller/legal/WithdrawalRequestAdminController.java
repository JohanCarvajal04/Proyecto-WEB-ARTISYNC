package uteq.edu.ec.artisync.controller.legal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalRequestFilter;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalDecisionRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IWithdrawalRequestService;

/**
 * Cola de revisión de retiros del Auditor Financiero. RETIROS_GESTIONAR se
 * asigna a AUDITOR_FINANCIERO, no a ADMIN (mismo criterio que
 * EscrowPaymentAuditController con PAGO_AUDITAR); "or hasRole('ADMIN')"
 * es el comodín que ya usa el resto de endpoints financieros del panel admin.
 */
@RestController
@RequestMapping("/api/v1/admin/retiros")
@RequiredArgsConstructor
public class WithdrawalRequestAdminController {

    private final IWithdrawalRequestService solicitudRetiroServicio;

    /**
     * Lista de forma paginada la cola de solicitudes de retiro, con filtros opcionales.
     *
     * @param filtro criterios opcionales para filtrar la cola de solicitudes
     * @param pageable configuración de paginación
     * @return página con las solicitudes de retiro que cumplen el filtro
     */
    @GetMapping
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<WithdrawalRequestResponse>> list(WithdrawalRequestFilter filtro, Pageable pageable) {
        return ResponseEntity.ok(solicitudRetiroServicio.listQueue(filtro, pageable));
    }

    /**
     * Aprueba una solicitud de retiro pendiente y ejecuta el pago vía PayPal Payouts.
     *
     * @param idSolicitud identificador de la solicitud de retiro
     * @param userDetails administrador autenticado que aprueba la solicitud
     * @return la solicitud de retiro con su estado actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el administrador no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la solicitud no está en estado pendiente
     */
    @PostMapping("/{idSolicitud}/aprobar")
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> approve(
            @PathVariable Long idSolicitud,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.approve(idSolicitud, userDetails.getIdUsuario()));
    }

    /**
     * Rechaza una solicitud de retiro pendiente.
     *
     * @param idSolicitud identificador de la solicitud de retiro
     * @param userDetails administrador autenticado que rechaza la solicitud
     * @param peticion nota administrativa con el motivo del rechazo
     * @return la solicitud de retiro con su estado actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el administrador no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si no se indica motivo de rechazo, o la solicitud no está en estado pendiente
     */
    @PostMapping("/{idSolicitud}/rechazar")
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> reject(
            @PathVariable Long idSolicitud,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WithdrawalDecisionRequest peticion) {
        return ResponseEntity.ok(solicitudRetiroServicio.reject(
                idSolicitud, userDetails.getIdUsuario(), peticion.getNotaAdmin()));
    }

    /**
     * Solo tiene efecto sobre una solicitud en estado "Fallido" (ver IWithdrawalRequestService.retry).
     *
     * @param idSolicitud identificador de la solicitud de retiro
     * @param userDetails administrador autenticado que reintenta la solicitud
     * @return la solicitud de retiro con su estado actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el administrador no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la solicitud no está en estado fallido
     */
    @PostMapping("/{idSolicitud}/reintentar")
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<WithdrawalRequestResponse> retry(
            @PathVariable Long idSolicitud,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.retry(idSolicitud, userDetails.getIdUsuario()));
    }
}
