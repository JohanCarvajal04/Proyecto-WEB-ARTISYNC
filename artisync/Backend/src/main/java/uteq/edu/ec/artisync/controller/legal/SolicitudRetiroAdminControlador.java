package uteq.edu.ec.artisync.controller.legal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroSolicitudRetiro;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionDecisionRetiro;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSolicitudRetiro;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.ISolicitudRetiroServicio;

/**
 * Cola de revisión de retiros del Auditor Financiero. RETIROS_GESTIONAR se
 * asigna a AUDITOR_FINANCIERO, no a ADMIN (mismo criterio que
 * PagoGarantiaAuditoriaControlador con PAGO_AUDITAR); "or hasRole('ADMIN')"
 * es el comodín que ya usa el resto de endpoints financieros del panel admin.
 */
@RestController
@RequestMapping("/api/v1/admin/retiros")
@RequiredArgsConstructor
public class SolicitudRetiroAdminControlador {

    private final ISolicitudRetiroServicio solicitudRetiroServicio;

    @GetMapping
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<RespuestaSolicitudRetiro>> listar(FiltroSolicitudRetiro filtro, Pageable pageable) {
        return ResponseEntity.ok(solicitudRetiroServicio.listarCola(filtro, pageable));
    }

    @PostMapping("/{idSolicitud}/aprobar")
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaSolicitudRetiro> aprobar(
            @PathVariable Long idSolicitud,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.aprobar(idSolicitud, userDetails.getIdUsuario()));
    }

    @PostMapping("/{idSolicitud}/rechazar")
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaSolicitudRetiro> rechazar(
            @PathVariable Long idSolicitud,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionDecisionRetiro peticion) {
        return ResponseEntity.ok(solicitudRetiroServicio.rechazar(
                idSolicitud, userDetails.getIdUsuario(), peticion.getNotaAdmin()));
    }

    /** Solo tiene efecto sobre una solicitud en estado "Fallido" (ver ISolicitudRetiroServicio.reintentar). */
    @PostMapping("/{idSolicitud}/reintentar")
    @PreAuthorize("hasAuthority('RETIROS_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaSolicitudRetiro> reintentar(
            @PathVariable Long idSolicitud,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.reintentar(idSolicitud, userDetails.getIdUsuario()));
    }
}
