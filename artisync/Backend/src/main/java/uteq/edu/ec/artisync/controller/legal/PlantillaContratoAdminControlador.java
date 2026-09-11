package uteq.edu.ec.artisync.controller.legal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaContrato;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaContrato;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;
import uteq.edu.ec.artisync.service.legal.IPlantillaContratoAdminServicio;

import java.util.List;

/**
 * Catálogo de plantillas de contrato curado por ADMIN (REQ-F-017 ampliado):
 * el creador solo elige entre estas plantillas, no escribe texto legal libre.
 */
@RestController
@RequestMapping("/api/v1/admin/plantillas-contrato")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CONTRATO_PLANTILLA_GESTIONAR')")
public class PlantillaContratoAdminControlador {

    private final IPlantillaContratoAdminServicio plantillaContratoAdminServicio;

    /**
     * Crea una nueva plantilla de contrato en el catálogo curado por administración.
     *
     * @param peticion datos de la plantilla a crear
     * @return la plantilla creada, con estado 201
     * @throws BusinessRuleException si ya existe una plantilla con la misma versión legal
     */
    @PostMapping
    public ResponseEntity<RespuestaPlantillaContrato> crear(@Valid @RequestBody PeticionCrearPlantillaContrato peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaContratoAdminServicio.crear(peticion));
    }

    /**
     * Edita una plantilla de contrato existente.
     *
     * @param id identificador de la plantilla a editar
     * @param peticion datos actualizados de la plantilla
     * @return la plantilla actualizada
     * @throws ResourceNotFoundException si la plantilla no existe
     * @throws BusinessRuleException si la plantilla no puede editarse en su estado actual
     */
    @PutMapping("/{id}")
    public ResponseEntity<RespuestaPlantillaContrato> editar(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarPlantillaContrato peticion) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.editar(id, peticion));
    }

    /**
     * Lista todas las plantillas de contrato del catálogo, activas e inactivas.
     *
     * @return listado completo de plantillas de contrato
     */
    @GetMapping
    public ResponseEntity<List<RespuestaPlantillaContrato>> listarTodas() {
        return ResponseEntity.ok(plantillaContratoAdminServicio.listarTodas());
    }

    /**
     * Desactiva una plantilla de contrato del catálogo.
     *
     * @param id identificador de la plantilla a desactivar
     * @return mensaje de confirmación de la desactivación
     * @throws ResourceNotFoundException si la plantilla no existe
     * @throws BusinessRuleException si la plantilla no puede desactivarse en su estado actual
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<RespuestaMensaje> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.desactivar(id));
    }
}
