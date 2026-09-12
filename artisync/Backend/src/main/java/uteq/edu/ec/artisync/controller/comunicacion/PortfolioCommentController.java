package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateCommentRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.CommentResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.PortfolioCommentService;

import java.util.Map;

/**
 * Controlador de comentarios sobre ítems de portafolio.
 */
@Tag(name = "Comentarios", description = "Comentarios en ítems de portafolio")
@RestController
@RequestMapping("/api/v1/portafolio-items")
@RequiredArgsConstructor
public class PortfolioCommentController {

    private final PortfolioCommentService comentarioService;

    /**
     * Crea un comentario sobre un ítem de portafolio.
     *
     * @param idItemPortafolio identificador del ítem de portafolio comentado
     * @param peticion contenido del comentario
     * @param userDetails usuario autenticado que comenta
     * @return el comentario creado, con estado 201
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el ítem de portafolio o el usuario autor no existen
     */
    @Operation(summary = "Comentar un ítem de portafolio")
    @PostMapping("/{idItemPortafolio}/comentarios")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long idItemPortafolio,
            @Valid @RequestBody CreateCommentRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.createComment(idItemPortafolio, peticion, userDetails.getIdUsuario()));
    }

    /**
     * Lista los comentarios activos de un ítem de portafolio, de acceso público.
     *
     * @param idItemPortafolio identificador del ítem de portafolio
     * @param pageable configuración de paginación
     * @return página con los comentarios activos del ítem
     */
    @Operation(summary = "Listar comentarios activos de un ítem de portafolio (público)")
    @GetMapping("/{idItemPortafolio}/comentarios")
    public ResponseEntity<Page<CommentResponse>> listComments(
            @PathVariable Long idItemPortafolio,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comentarioService.listComments(idItemPortafolio, pageable));
    }

    /**
     * Cuenta los comentarios de un ítem de portafolio, de acceso público.
     *
     * @param idItemPortafolio identificador del ítem de portafolio
     * @return el identificador del ítem y el total de comentarios
     */
    @Operation(summary = "Contar comentarios de un ítem de portafolio (público)")
    @GetMapping("/{idItemPortafolio}/comentarios/conteo")
    public ResponseEntity<Map<String, Object>> countComments(@PathVariable Long idItemPortafolio) {
        return ResponseEntity.ok(Map.of(
                "idItemPortafolio", idItemPortafolio,
                "total", comentarioService.countComments(idItemPortafolio)
        ));
    }

    /**
     * Elimina un comentario propio, del dueño del portafolio comentado, o como administrador.
     *
     * @param idComentario identificador del comentario a eliminar
     * @param userDetails usuario autenticado que solicita la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el comentario no existe
     */
    @Operation(summary = "Eliminar un comentario propio, del portafolio, o como ADMIN")
    @DeleteMapping("/comentarios/{idComentario}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long idComentario,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean esAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        comentarioService.deleteComment(idComentario, userDetails.getIdUsuario(), esAdmin);
    }
}
