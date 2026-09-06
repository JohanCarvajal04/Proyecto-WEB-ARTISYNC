package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subcategorias")
@RequiredArgsConstructor
public class SubcategoriaControlador {

    private final ICategoriaServicio categoriaServicio;

    @GetMapping
    public ResponseEntity<List<RespuestaSubcategoria>> listarTodasLasSubcategorias() {
        return ResponseEntity.ok(categoriaServicio.listarTodasLasSubcategorias());
    }

    // CATEGORIA_CREAR es autoservicio (cada creador crea las suyas, quedan sin
    // revisar); CATEGORIA_GESTIONAR/ADMIN crean directamente como revisadas,
    // igual que siempre.
    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasAuthority('CATEGORIA_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaSubcategoria> crearSubcategoria(
            @Valid @RequestBody PeticionCrearSubcategoria peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioCreador = esModerador(userDetails) ? null : userDetails.getIdUsuario();
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaServicio.crearSubcategoria(idUsuarioCreador, peticion));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarSubcategoria(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        categoriaServicio.eliminarSubcategoria(id, motivo);
        return ResponseEntity.ok(new RespuestaMensaje("Subcategoria eliminada exitosamente"));
    }

    @GetMapping("/pendientes-revision")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaSubcategoria>> listarPendientesRevision() {
        return ResponseEntity.ok(categoriaServicio.listarSubcategoriasPendientesRevision());
    }

    @PatchMapping("/{id}/revisar")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaSubcategoria> marcarRevisada(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaServicio.marcarSubcategoriaRevisada(id));
    }

    private boolean esModerador(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CATEGORIA_GESTIONAR") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
