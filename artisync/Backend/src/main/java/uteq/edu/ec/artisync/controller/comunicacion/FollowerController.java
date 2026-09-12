package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowedCreatorUpdateResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowerResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.IFollowerService;

import java.util.List;

/** Seguimiento (follow/unfollow) de perfiles de creador. */
@Tag(name = "Seguidores", description = "Gestión de seguimiento a perfiles de creadores")
@RestController
@RequestMapping("/api/v1/creadores")
@RequiredArgsConstructor
public class FollowerController {

    private final IFollowerService seguidorServicio;

    /**
     * Registra al usuario autenticado como seguidor del perfil de creador indicado.
     * @param idPerfil identificador del perfil de creador a seguir
     * @param userDetails identidad del usuario autenticado que inicia el seguimiento
     * @return el nuevo estado de seguimiento y el conteo actualizado de seguidores
     */
    @Operation(summary = "Seguir a un perfil de creador")
    @PostMapping("/{idPerfil}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FollowStatusResponse> followCreator(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorServicio.followCreator(userDetails.getIdUsuario(), idPerfil));
    }

    /**
     * Elimina la relación de seguimiento del usuario autenticado hacia el perfil de creador indicado.
     * @param idPerfil identificador del perfil de creador que se deja de seguir
     * @param userDetails identidad del usuario autenticado que termina el seguimiento
     * @return el nuevo estado de seguimiento y el conteo actualizado de seguidores
     */
    @Operation(summary = "Dejar de seguir a un perfil de creador")
    @DeleteMapping("/{idPerfil}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FollowStatusResponse> unfollowCreator(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorServicio.unfollowCreator(userDetails.getIdUsuario(), idPerfil));
    }

    /**
     * Consulta si el usuario actual (si hay sesión) sigue al perfil de creador indicado, junto con el total de seguidores.
     * @param idPerfil identificador del perfil de creador a consultar
     * @param userDetails identidad del usuario autenticado, o null si la consulta es anónima
     * @return el estado de seguimiento del usuario actual y el conteo total de seguidores del perfil
     */
    @Operation(summary = "Obtener el estado de seguimiento y conteo de un perfil de creador")
    @GetMapping("/{idPerfil}/es-seguidor")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuario = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(seguidorServicio.getFollowStatus(idUsuario, idPerfil));
    }

    /**
     * Obtiene la lista pública de usuarios que siguen al perfil de creador indicado.
     * @param idPerfil identificador del perfil de creador cuyos seguidores se listan
     * @return los seguidores registrados del perfil
     */
    @Operation(summary = "Listar los seguidores de un perfil de creador")
    @GetMapping("/{idPerfil}/seguidores")
    public ResponseEntity<List<FollowerResponse>> listFollowers(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(seguidorServicio.listFollowers(idPerfil));
    }

    /**
     * Obtiene el feed de novedades publicadas por los creadores que sigue el usuario autenticado.
     * @param userDetails identidad del usuario autenticado cuyos creadores seguidos se consultan
     * @return las novedades recientes de los creadores seguidos
     */
    @Operation(summary = "Listar las novedades de los creadores que el usuario sigue")
    @GetMapping("/siguiendo/novedades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FollowedCreatorUpdateResponse>> listFollowedCreatorUpdates(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorServicio.listFollowedCreatorUpdates(userDetails.getIdUsuario()));
    }

    /**
     * Actualiza la imagen de portada y el título profesional del perfil de creador autenticado.
     * @param urlPortada nueva URL de la imagen de portada del perfil, o null para no modificarla
     * @param tituloProfesional nuevo título profesional a mostrar en el perfil, o null para no modificarlo
     * @param userDetails identidad del creador autenticado cuyo perfil se actualiza
     * @return mensaje de confirmación de la actualización
     */
    @Operation(summary = "Actualizar la portada y título profesional del creador")
    @PutMapping("/mi-perfil/portada")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> updateCoverAndTitle(
            @RequestParam(required = false) String urlPortada,
            @RequestParam(required = false) String tituloProfesional,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        seguidorServicio.updateCoverAndTitle(userDetails.getIdUsuario(), urlPortada, tituloProfesional);
        return ResponseEntity.ok(new RespuestaMensaje("Portada y especialidad del perfil actualizadas correctamente."));
    }
}
