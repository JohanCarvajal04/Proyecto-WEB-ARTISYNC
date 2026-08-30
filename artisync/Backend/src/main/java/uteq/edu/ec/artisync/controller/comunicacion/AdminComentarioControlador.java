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
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.service.comunicacion.ComentarioPortafolioService;

/**
 * Controlador de administración de comentarios de portafolio.
 * COMENTARIO_MODERAR (o ADMIN) puede listar, ocultar y reactivar comentarios;
 * el borrado definitivo queda reservado a ADMIN, igual que en portafolios.
 */
@Tag(name = "Admin — Comentarios", description = "Moderación de comentarios de portafolio")
@RestController
@RequestMapping("/api/v1/admin/comentarios")
@RequiredArgsConstructor
public class AdminComentarioControlador {

    private final ComentarioPortafolioService comentarioService;

    @Operation(summary = "Listar todos los comentarios del sistema")
    @GetMapping
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<RespuestaComentario>> listarParaModeracion(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comentarioService.listarParaModeracion(pageable));
    }

    @Operation(summary = "Ocultar un comentario (moderación)")
    @PatchMapping("/{idComentario}/ocultar")
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaComentario> ocultarComentario(@PathVariable Long idComentario) {
        return ResponseEntity.ok(comentarioService.ocultarComentario(idComentario));
    }

    @Operation(summary = "Reactivar un comentario previamente oculto")
    @PatchMapping("/{idComentario}/reactivar")
    @PreAuthorize("hasAuthority('COMENTARIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaComentario> reactivarComentario(@PathVariable Long idComentario) {
        return ResponseEntity.ok(comentarioService.reactivarComentario(idComentario));
    }

    @Operation(summary = "Eliminar definitivamente un comentario (moderación)")
    @DeleteMapping("/{idComentario}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarComentario(@PathVariable Long idComentario) {
        comentarioService.eliminarComentario(idComentario, null, true);
        return ResponseEntity.noContent().build();
    }
}
