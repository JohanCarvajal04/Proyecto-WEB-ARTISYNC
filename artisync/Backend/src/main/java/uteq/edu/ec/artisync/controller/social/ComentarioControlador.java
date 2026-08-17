package uteq.edu.ec.artisync.controller.social;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ComentarioService;

@Tag(name = "Comentarios Portafolio", description = "Gestión de comentarios en obras de portafolio")
@RestController
@RequiredArgsConstructor
public class ComentarioControlador {

    private final ComentarioService comentarioService;

    @Operation(summary = "Agregar un comentario a un ítem de portafolio")
    @PostMapping("/api/v1/portafolio/items/{idItem}/comentarios")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaComentario> agregarComentario(
            @PathVariable Long idItem,
            @Valid @RequestBody PeticionCrearComentario peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.agregarComentario(userDetails.getIdUsuario(), idItem, peticion));
    }

    @Operation(summary = "Eliminar un comentario (borrado lógico)")
    @DeleteMapping("/api/v1/comentarios/{idComentario}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> eliminarComentario(
            @PathVariable Long idComentario,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(comentarioService.eliminarComentario(userDetails.getIdUsuario(), idComentario));
    }

    @Operation(summary = "Listar comentarios activos de un ítem")
    @GetMapping("/api/v1/portafolio/items/{idItem}/comentarios")
    public ResponseEntity<Page<RespuestaComentario>> listarComentariosPorItem(
            @PathVariable Long idItem,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPublicacion"));
        return ResponseEntity.ok(comentarioService.listarComentariosPorItem(idItem, pageable));
    }
}
