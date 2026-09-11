package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.LikeStatusResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.PortfolioLikeService;

/**
 * Controlador de "me gusta" sobre ítems de portafolio.
 */
@Tag(name = "Likes", description = "Me gusta en ítems de portafolio")
@RestController
@RequestMapping("/api/v1/portafolio-items")
@RequiredArgsConstructor
public class PortfolioLikeController {

    private final PortfolioLikeService likeService;

    /**
     * Da "me gusta" a un ítem de portafolio en nombre del usuario autenticado.
     *
     * @param idItemPortafolio identificador del ítem de portafolio
     * @param userDetails usuario autenticado que da el like
     * @return el estado de likes actualizado, con estado 201
     * @throws DuplicateResourceException si el usuario ya le dio like al ítem
     * @throws ResourceNotFoundException si el usuario no existe
     */
    @Operation(summary = "Dar like a un ítem de portafolio")
    @PostMapping("/{idItemPortafolio}/likes")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<LikeStatusResponse> darLike(
            @PathVariable Long idItemPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(likeService.darLike(idItemPortafolio, userDetails.getIdUsuario()));
    }

    /**
     * Quita el "me gusta" del usuario autenticado sobre un ítem de portafolio.
     *
     * @param idItemPortafolio identificador del ítem de portafolio
     * @param userDetails usuario autenticado que quita el like
     * @return el estado de likes actualizado
     * @throws ResourceNotFoundException si el usuario no le había dado like al ítem
     */
    @Operation(summary = "Quitar el like de un ítem de portafolio")
    @DeleteMapping("/{idItemPortafolio}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeStatusResponse> quitarLike(
            @PathVariable Long idItemPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(likeService.quitarLike(idItemPortafolio, userDetails.getIdUsuario()));
    }

    /**
     * Obtiene el estado de likes de un ítem de portafolio, de acceso público.
     *
     * @param idItemPortafolio identificador del ítem de portafolio
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return el estado de likes del ítem, incluyendo si el usuario actual ya dio like
     * @throws ResourceNotFoundException si el ítem de portafolio no existe
     */
    @Operation(summary = "Estado de likes de un ítem de portafolio (público)")
    @GetMapping("/{idItemPortafolio}/likes")
    public ResponseEntity<LikeStatusResponse> obtenerEstado(
            @PathVariable Long idItemPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuario = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(likeService.obtenerEstado(idItemPortafolio, idUsuario));
    }
}
