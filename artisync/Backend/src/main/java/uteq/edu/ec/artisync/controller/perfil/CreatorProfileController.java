package uteq.edu.ec.artisync.controller.perfil;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateProfileRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdateProfileRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.ProfileResponse;
import uteq.edu.ec.artisync.service.perfil.ICreatorProfileService;

import java.util.List;

/** Consulta y edición del perfil público de un creador. */
@RestController
@RequestMapping("/api/v1/perfiles")
@RequiredArgsConstructor
public class CreatorProfileController {

    private final ICreatorProfileService perfilServicio;

    /**
     * Crea el perfil de creador del usuario autenticado (o de un tercero,
     * cuando quien llama tiene rol ADMIN).
     * @param peticion datos del perfil de creador a registrar
     * @param autenticacion usuario autenticado que solicita la creación
     * @return perfil de creador recién creado, con código 201
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ProfileResponse> crearPerfil(
            @Valid @RequestBody CreateProfileRequest peticion,
            Authentication autenticacion) {
        ProfileResponse respuesta = perfilServicio.crearPerfil(
                peticion, autenticacion.getName(), esAdmin(autenticacion));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene el perfil de creador identificado por su clave primaria.
     * @param id identificador del perfil de creador
     * @return perfil de creador correspondiente
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> obtenerPerfilPorId(@PathVariable Long id) {
        return ResponseEntity.ok(perfilServicio.obtenerPerfilPorId(id));
    }

    /**
     * Obtiene el perfil de creador asociado a una cuenta de usuario.
     * @param idUsuario identificador del usuario dueño del perfil
     * @return perfil de creador de ese usuario
     */
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<ProfileResponse> obtenerPerfilPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(perfilServicio.obtenerPerfilPorUsuario(idUsuario));
    }

    /**
     * Lista todos los perfiles de creador registrados en la plataforma.
     * @return listado completo de perfiles de creador
     */
    @GetMapping
    public ResponseEntity<List<ProfileResponse> > listarPerfiles() {
        return ResponseEntity.ok(perfilServicio.listarPerfiles());
    }

    /**
     * Lista los perfiles de creador con cuenta activa, para el directorio
     * público de creadores.
     * @return listado de perfiles de creadores activos
     */
    @Operation(summary = "Directorio público de creadores con cuenta activa")
    @GetMapping("/activos")
    public ResponseEntity<List<ProfileResponse>> listarPerfilesActivos() {
        return ResponseEntity.ok(perfilServicio.listarPerfilesActivos());
    }

    /**
     * Actualiza los datos del perfil de creador indicado; solo el dueño del
     * perfil o un ADMIN pueden hacerlo.
     * @param id identificador del perfil a actualizar
     * @param peticion datos actualizados del perfil
     * @param autenticacion usuario autenticado que solicita el cambio
     * @return perfil de creador ya actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ProfileResponse> actualizarPerfil(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest peticion,
            Authentication autenticacion) {
        return ResponseEntity.ok(perfilServicio.actualizarPerfil(
                id, peticion, autenticacion.getName(), esAdmin(autenticacion)));
    }

    /**
     * Elimina el perfil de creador indicado.
     * @param id identificador del perfil a eliminar
     * @return mensaje de confirmación de la eliminación
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ELIMINAR') or hasRole('ADMIN')")
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
