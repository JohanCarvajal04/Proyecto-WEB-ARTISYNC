package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;
import uteq.edu.ec.artisync.service.perfil.IPortafolioServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portafolios")
@RequiredArgsConstructor
public class PortafolioControlador {

    private final IPortafolioServicio portafolioServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPortafolio> crearPortafolio(@Valid @RequestBody PeticionCrearPortafolio peticion) {
        RespuestaPortafolio respuesta = portafolioServicio.crearPortafolio(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaPortafolio> obtenerPortafolioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(portafolioServicio.obtenerPortafolioPorId(id));
    }

    @GetMapping("/perfil/{idPerfil}")
    public ResponseEntity<RespuestaPortafolio> obtenerPortafolioPorPerfil(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(portafolioServicio.obtenerPortafolioPorPerfil(idPerfil));
    }

    @GetMapping
    public ResponseEntity<List<RespuestaPortafolio> > listarPortafolios() {
        return ResponseEntity.ok(portafolioServicio.listarPortafolios());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPortafolio> actualizarPortafolio(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarPortafolio peticion) {
        return ResponseEntity.ok(portafolioServicio.actualizarPortafolio(id, peticion));
    }

    @PostMapping("/{id}/visita")
    public ResponseEntity<RespuestaMensaje> registrarVisita(@PathVariable Long id) {
        portafolioServicio.incrementarVisitas(id);
        return ResponseEntity.ok(new RespuestaMensaje("Visita al portafolio incrementada"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarPortafolio(@PathVariable Long id) {
        portafolioServicio.eliminarPortafolio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Portafolio eliminado exitosamente"));
    }
}
