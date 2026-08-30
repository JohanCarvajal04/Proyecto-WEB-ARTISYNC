package uteq.edu.ec.artisync.controller.perfil;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<RespuestaPerfil> crearPerfil(
            @Valid @RequestBody PeticionCrearPerfil peticion,
            Authentication autenticacion) {
        RespuestaPerfil respuesta = perfilServicio.crearPerfil(
                peticion, autenticacion.getName(), esAdmin(autenticacion));
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

    @Operation(summary = "Directorio público de creadores con cuenta activa")
    @GetMapping("/activos")
    public ResponseEntity<List<RespuestaPerfil>> listarPerfilesActivos() {
        return ResponseEntity.ok(perfilServicio.listarPerfilesActivos());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPerfil> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarPerfil peticion,
            Authentication autenticacion) {
        return ResponseEntity.ok(perfilServicio.actualizarPerfil(
                id, peticion, autenticacion.getName(), esAdmin(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarPerfil(@PathVariable Long id) {
        perfilServicio.eliminarPerfil(id);
        return ResponseEntity.ok(new RespuestaMensaje("Perfil de creador eliminado exitosamente"));
    }

    /**
     * Un ADMIN puede operar sobre el perfil de cualquiera; el resto solo sobre el
     * suyo. Se resuelve aquí y se pasa al servicio como booleano para que este no
     * dependa del SecurityContextHolder y siga siendo comprobable con un test
     * unitario corriente.
     */
    private boolean esAdmin(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
