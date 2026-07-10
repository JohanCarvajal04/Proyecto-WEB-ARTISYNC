package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarServicio;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaAtributo;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioControlador {

    private final IServicioCatalogoServicio servicioCatalogoServicio;

    @PostMapping("/creador/{idPerfilCreador}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaServicio> crearServicio(
            @PathVariable Long idPerfilCreador,
            @Valid @RequestBody PeticionCrearServicio peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.crearServicio(idPerfilCreador, peticion));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaServicio> actualizarServicio(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarServicio peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarServicio(id, peticion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaServicio> obtenerServicioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.obtenerServicioPorId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarServicio(@PathVariable Long id) {
        servicioCatalogoServicio.eliminarServicio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Servicio eliminado exitosamente"));
    }

    @GetMapping("/creador/{idPerfilCreador}")
    public ResponseEntity<List<RespuestaServicioResumido>> listarServiciosPorCreador(
            @PathVariable Long idPerfilCreador,
            @RequestParam(required = false) String estadoPublicacion) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarServiciosPorCreador(idPerfilCreador, estadoPublicacion));
    }

    @GetMapping("/{id}/atributos")
    public ResponseEntity<List<RespuestaAtributo>> listarAtributosPorServicio(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarAtributosPorServicio(id));
    }

    @PostMapping("/{id}/atributos")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaAtributo> agregarAtributo(
            @PathVariable Long id,
            @Valid @RequestBody PeticionCrearAtributo peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.agregarAtributo(id, peticion));
    }

    @PutMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaAtributo> actualizarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo,
            @Valid @RequestBody PeticionActualizarAtributo peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarAtributo(id, idAtributo, peticion));
    }

    @DeleteMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo) {
        servicioCatalogoServicio.eliminarAtributo(id, idAtributo);
        return ResponseEntity.ok(new RespuestaMensaje("Atributo eliminado exitosamente del servicio"));
    }
}
