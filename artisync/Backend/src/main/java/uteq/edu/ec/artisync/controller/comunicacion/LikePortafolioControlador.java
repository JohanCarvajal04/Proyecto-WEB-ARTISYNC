package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.LikePortafolioService;

/**
 * Controlador de "me gusta" sobre ítems de portafolio.
 */
@Tag(name = "Likes", description = "Me gusta en ítems de portafolio")
@RestController
@RequestMapping("/api/v1/portafolio-items")
@RequiredArgsConstructor
public class LikePortafolioControlador {

    private final LikePortafolioService likeService;

    @Operation(summary = "Dar like a un ítem de portafolio")
    @PostMapping("/{idItemPortafolio}/likes")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaEstadoLike> darLike(
            @PathVariable Long idItemPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(likeService.darLike(idItemPortafolio, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Quitar el like de un ítem de portafolio")
    @DeleteMapping("/{idItemPortafolio}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaEstadoLike> quitarLike(
            @PathVariable Long idItemPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(likeService.quitarLike(idItemPortafolio, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Estado de likes de un ítem de portafolio (público)")
    @GetMapping("/{idItemPortafolio}/likes")
    public ResponseEntity<RespuestaEstadoLike> obtenerEstado(
            @PathVariable Long idItemPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuario = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(likeService.obtenerEstado(idItemPortafolio, idUsuario));
    }
}
