package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;
import uteq.edu.ec.artisync.service.pedido.IFlujoTrabajoServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flujos")
@RequiredArgsConstructor
public class FlujoTrabajoControlador {

    private final IFlujoTrabajoServicio flujoTrabajoServicio;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> crearFlujo(
            @Valid @RequestBody PeticionCrearFlujoTrabajo peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flujoTrabajoServicio.crearFlujoTrabajo(peticion));
    }

    @GetMapping
    public ResponseEntity<List<RespuestaFlujoTrabajo>> listarFlujos() {
        return ResponseEntity.ok(flujoTrabajoServicio.listarFlujosTrabajo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaFlujoTrabajo> obtenerFlujo(@PathVariable Long id) {
        return ResponseEntity.ok(flujoTrabajoServicio.obtenerFlujoPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> actualizarFlujo(
            @PathVariable Long id,
            @Valid @RequestBody PeticionCrearFlujoTrabajo peticion) {
        return ResponseEntity.ok(flujoTrabajoServicio.actualizarFlujoTrabajo(id, peticion));
    }

    @PostMapping("/{id}/etapas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> agregarEtapa(
            @PathVariable Long id,
            @Valid @RequestBody PeticionEtapaConfig peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flujoTrabajoServicio.agregarEtapa(id, peticion));
    }

    @PutMapping("/{id}/etapas/{etapaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> actualizarEtapa(
            @PathVariable Long id,
            @PathVariable Long etapaId,
            @Valid @RequestBody PeticionEtapaConfig peticion) {
        return ResponseEntity.ok(flujoTrabajoServicio.actualizarEtapa(id, etapaId, peticion));
    }

    @DeleteMapping("/{id}/etapas/{etapaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarEtapa(
            @PathVariable Long id,
            @PathVariable Long etapaId) {
        flujoTrabajoServicio.eliminarEtapa(id, etapaId);
        return ResponseEntity.ok(new RespuestaMensaje("Etapa eliminada exitosamente del flujo de trabajo"));
    }
}
