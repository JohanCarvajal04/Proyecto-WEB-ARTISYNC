package uteq.edu.ec.artisync.controller.legal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.service.legal.IContractTemplateAdminService;

import java.util.List;

/**
 * Catálogo de plantillas de contrato curado por ADMIN (REQ-F-017 ampliado):
 * el creador solo elige entre estas plantillas, no escribe texto legal libre.
 */
@RestController
@RequestMapping("/api/v1/admin/plantillas-contrato")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CONTRATO_PLANTILLA_GESTIONAR')")
public class ContractTemplateAdminController {

    private final IContractTemplateAdminService plantillaContratoAdminServicio;

    /**
     * Crea una nueva plantilla de contrato en el catálogo curado por administración.
     *
     * @param peticion datos de la plantilla a create
     * @return la plantilla creada, con estado 201
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una plantilla con la misma versión legal
     */
    @PostMapping
    public ResponseEntity<ContractTemplateResponse> create(@Valid @RequestBody CreateContractTemplateRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaContratoAdminServicio.create(peticion));
    }

    /**
     * Edita una plantilla de contrato existente.
     *
     * @param id identificador de la plantilla a update
     * @param peticion datos actualizados de la plantilla
     * @return la plantilla actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la plantilla no puede editarse en su estado actual
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContractTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContractTemplateRequest peticion) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.update(id, peticion));
    }

    /**
     * Lista todas las plantillas de contrato del catálogo, activas e inactivas.
     *
     * @return listado completo de plantillas de contrato
     */
    @GetMapping
    public ResponseEntity<List<ContractTemplateResponse>> listAll() {
        return ResponseEntity.ok(plantillaContratoAdminServicio.listAll());
    }

    /**
     * Desactiva una plantilla de contrato del catálogo.
     *
     * @param id identificador de la plantilla a deactivate
     * @return mensaje de confirmación de la desactivación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la plantilla no puede desactivarse en su estado actual
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<RespuestaMensaje> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.deactivate(id));
    }
}
