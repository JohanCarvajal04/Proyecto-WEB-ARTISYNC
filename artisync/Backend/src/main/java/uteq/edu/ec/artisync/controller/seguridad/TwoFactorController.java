package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorConfirmRequest;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.TwoFactorSetupResponse;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;

import java.security.Principal;

@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
@Tag(name = "Autenticación 2FA", description = "Endpoints para gestionar la autenticación de dos factores (TOTP) y códigos de respaldo")
@SecurityRequirement(name = "bearerAuth")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @Operation(summary = "Iniciar configuración de 2FA (obtiene secreto, URI de QR y códigos de respaldo)")
    @PostMapping("/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2Fa(Principal principal) {
        return ResponseEntity.ok(twoFactorService.setup2Fa(principal.getName()));
    }

    @Operation(summary = "Confirmar y activar 2FA verificando el primer código TOTP generado por el autenticador")
    @PostMapping("/confirm")
    public ResponseEntity<MessageResponse> confirm2Fa(Principal principal, @Valid @RequestBody TwoFactorConfirmRequest request) {
        return ResponseEntity.ok(twoFactorService.confirm2Fa(principal.getName(), request.getCodigo()));
    }

    @Operation(summary = "Desactivar 2FA verificando con un código TOTP o de respaldo")
    @DeleteMapping("/disable")
    public ResponseEntity<MessageResponse> disable2Fa(Principal principal, @Valid @RequestBody TwoFactorConfirmRequest request) {
        return ResponseEntity.ok(twoFactorService.disable2Fa(principal.getName(), request.getCodigo()));
    }
}
