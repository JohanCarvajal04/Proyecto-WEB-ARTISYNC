package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContratoResumen;
import uteq.edu.ec.artisync.service.legal.IPlantillaContratoAdminServicio;

import java.util.List;

/** Lectura para el creador: elegir una plantilla activa al crear/editar su servicio. */
@RestController
@RequestMapping("/api/v1/plantillas-contrato")
@RequiredArgsConstructor
public class PlantillaContratoControlador {

    private final IPlantillaContratoAdminServicio plantillaContratoAdminServicio;

    @GetMapping("/activas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RespuestaPlantillaContratoResumen>> listarActivas() {
        return ResponseEntity.ok(plantillaContratoAdminServicio.listarActivas());
    }
}
