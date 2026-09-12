package uteq.edu.ec.artisync.controller.legal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.ICreatorAgreementTemplateService;

import java.util.List;

/**
 * Autoservicio de plantillas de acuerdo propias (V45): el creador redacta y
 * mantiene sus propias plantillas, aparte del catálogo general curado por
 * ADMIN ({@link ContractTemplateAdminController}). Cada plantilla creada
 * aquí solo la puede usar, update o deactivate su propio dueño.
 */
@RestController
@RequestMapping("/api/v1/creador/plantillas-acuerdo")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CONTRATO_PLANTILLA_PROPIA_GESTIONAR')")
public class CreatorAgreementTemplateController {

    private final ICreatorAgreementTemplateService plantillaAcuerdoCreadorServicio;

    /**
     * Crea una plantilla de acuerdo privada para el creador autenticado.
     *
     * @param peticion nombre y texto legal de la plantilla
     * @param userDetails usuario autenticado dueño de la nueva plantilla
     * @return la plantilla creada, con estado 201
     */
    @PostMapping
    public ResponseEntity<ContractTemplateResponse> create(
            @Valid @RequestBody CreateOwnAgreementTemplateRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantillaAcuerdoCreadorServicio.create(userDetails.getIdUsuario(), peticion));
    }

    /**
     * Edita una plantilla de acuerdo propia existente.
     *
     * @param id identificador de la plantilla a update
     * @param peticion datos actualizados de la plantilla
     * @param userDetails usuario autenticado
     * @return la plantilla actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe o no pertenece a este creador
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContractTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOwnAgreementTemplateRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(plantillaAcuerdoCreadorServicio.update(userDetails.getIdUsuario(), id, peticion));
    }

    /**
     * Lista las plantillas de acuerdo propias del creador autenticado, activas e inactivas.
     *
     * @param userDetails usuario autenticado
     * @return sus plantillas propias
     */
    @GetMapping
    public ResponseEntity<List<ContractTemplateResponse>> listOwn(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(plantillaAcuerdoCreadorServicio.listOwn(userDetails.getIdUsuario()));
    }

    /**
     * Desactiva una plantilla de acuerdo propia.
     *
     * @param id identificador de la plantilla a deactivate
     * @param userDetails usuario autenticado
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe o no pertenece a este creador
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<RespuestaMensaje> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(plantillaAcuerdoCreadorServicio.deactivate(userDetails.getIdUsuario(), id));
    }
}
