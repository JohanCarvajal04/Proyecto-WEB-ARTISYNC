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
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ComentarioPortafolioService;

import java.util.Map;

/**
 * Controlador de comentarios sobre ítems de portafolio.
 */
@Tag(name = "Comentarios", description = "Comentarios en ítems de portafolio")
@RestController
@RequestMapping("/api/v1/portafolio-items")
@RequiredArgsConstructor
public class ComentarioPortafolioControlador {

    private final ComentarioPortafolioService comentarioService;

    @Operation(summary = "Comentar un ítem de portafolio")
    @PostMapping("/{idItemPortafolio}/comentarios")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaComentario> crearComentario(
            @PathVariable Long idItemPortafolio,
            @Valid @RequestBody PeticionCrearComentario peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.crearComentario(idItemPortafolio, peticion, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Listar comentarios activos de un ítem de portafolio (público)")
    @GetMapping("/{idItemPortafolio}/comentarios")
    public ResponseEntity<Page<RespuestaComentario>> listarComentarios(
            @PathVariable Long idItemPortafolio,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comentarioService.listarComentarios(idItemPortafolio, pageable));
    }

    @Operation(summary = "Contar comentarios de un ítem de portafolio (público)")
    @GetMapping("/{idItemPortafolio}/comentarios/conteo")
    public ResponseEntity<Map<String, Object>> contarComentarios(@PathVariable Long idItemPortafolio) {
        return ResponseEntity.ok(Map.of(
                "idItemPortafolio", idItemPortafolio,
                "total", comentarioService.contarComentarios(idItemPortafolio)
        ));
    }

    @Operation(summary = "Eliminar un comentario propio, del portafolio, o como ADMIN")
    @DeleteMapping("/comentarios/{idComentario}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarComentario(
            @PathVariable Long idComentario,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean esAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        comentarioService.eliminarComentario(idComentario, userDetails.getIdUsuario(), esAdmin);
    }
}
