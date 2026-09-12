package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.CommentResponse;
import uteq.edu.ec.artisync.service.comunicacion.PortfolioCommentService;

/**
 * Controlador de administración de comentarios de portafolio.
 * COMENTARIO_MODERAR (o ADMIN) puede listar, ocultar y reactivar comentarios;
 * el borrado definitivo queda reservado a ADMIN, igual que en portafolios.
 */
@Tag(name = "Admin — Comentarios", description = "Moderación de comentarios de portafolio")
@RestController
@RequestMapping("/api/v1/admin/comentarios")
@RequiredArgsConstructor
public class AdminCommentController {

    private final PortfolioCommentService comentarioService;

    /**
     * Lista todos los comentarios del sistema de forma paginada para su moderación.
     *
     * @param pageable configuración de paginación
     * @return página con los comentarios del sistema
     */
    @Operation(summary = "Listar todos los comentarios del sistema")
    @GetMapping
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<CommentResponse>> listarParaModeracion(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comentarioService.listarParaModeracion(pageable));
    }

    /**
     * Oculta un comentario como acción de moderación.
     *
     * @param idComentario identificador del comentario a ocultar
     * @return el comentario con su estado de moderación actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el comentario no existe
     */
    @Operation(summary = "Ocultar un comentario (moderación)")
    @PatchMapping("/{idComentario}/ocultar")
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<CommentResponse> ocultarComentario(@PathVariable Long idComentario) {
        return ResponseEntity.ok(comentarioService.ocultarComentario(idComentario));
    }

    /**
     * Reactiva un comentario previamente oculto.
     *
     * @param idComentario identificador del comentario a reactivar
     * @return el comentario con su estado de moderación actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el comentario no existe
     */
    @Operation(summary = "Reactivar un comentario previamente oculto")
    @PatchMapping("/{idComentario}/reactivar")
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<CommentResponse> reactivarComentario(@PathVariable Long idComentario) {
        return ResponseEntity.ok(comentarioService.reactivarComentario(idComentario));
    }

    /**
     * Elimina definitivamente un comentario del sistema como acción de moderación.
     *
     * @param idComentario identificador del comentario a eliminar
     * @return respuesta vacía con estado 204
     */
    @Operation(summary = "Eliminar definitivamente un comentario (moderación)")
    @DeleteMapping("/{idComentario}")
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarComentario(@PathVariable Long idComentario) {
        comentarioService.eliminarComentario(idComentario, null, true);
        return ResponseEntity.noContent().build();
    }
}
