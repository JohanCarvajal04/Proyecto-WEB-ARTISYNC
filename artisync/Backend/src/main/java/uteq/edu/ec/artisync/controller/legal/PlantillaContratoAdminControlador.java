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

    @PostMapping
    public ResponseEntity<RespuestaPlantillaContrato> crear(@Valid @RequestBody PeticionCrearPlantillaContrato peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaContratoAdminServicio.crear(peticion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespuestaPlantillaContrato> editar(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarPlantillaContrato peticion) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.editar(id, peticion));
    }

    @GetMapping
    public ResponseEntity<List<RespuestaPlantillaContrato>> listarTodas() {
        return ResponseEntity.ok(plantillaContratoAdminServicio.listarTodas());
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<RespuestaMensaje> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.desactivar(id));
    }
}
