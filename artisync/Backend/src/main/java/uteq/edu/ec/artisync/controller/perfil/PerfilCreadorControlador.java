package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPerfil;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPerfil;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPerfil;
import uteq.edu.ec.artisync.service.perfil.IPerfilCreadorServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/perfiles")
@RequiredArgsConstructor
public class PerfilCreadorControlador {

    private final IPerfilCreadorServicio perfilServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPerfil> crearPerfil(@Valid @RequestBody PeticionCrearPerfil peticion) {
        RespuestaPerfil respuesta = perfilServicio.crearPerfil(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaPerfil> obtenerPerfilPorId(@PathVariable Long id) {
        return ResponseEntity.ok(perfilServicio.obtenerPerfilPorId(id));
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<RespuestaPerfil> obtenerPerfilPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(perfilServicio.obtenerPerfilPorUsuario(idUsuario));
    }

    @GetMapping
    public ResponseEntity<List<RespuestaPerfil> > listarPerfiles() {
        return ResponseEntity.ok(perfilServicio.listarPerfiles());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPerfil> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarPerfil peticion) {
        return ResponseEntity.ok(perfilServicio.actualizarPerfil(id, peticion));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarPerfil(@PathVariable Long id) {
        perfilServicio.eliminarPerfil(id);
        return ResponseEntity.ok(new RespuestaMensaje("Perfil de creador eliminado exitosamente"));
    }
}
