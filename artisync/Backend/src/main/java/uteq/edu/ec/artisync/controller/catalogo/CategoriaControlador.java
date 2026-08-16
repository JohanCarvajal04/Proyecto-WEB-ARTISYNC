package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaControlador {

    private final ICategoriaServicio categoriaServicio;

    @GetMapping
    public ResponseEntity<List<RespuestaCategoria>> listarCategoriasActivas() {
        return ResponseEntity.ok(categoriaServicio.listarCategoriasActivas());
    }

    // La gestión del catálogo se autoriza por permiso, no por rol: MODERADOR
    // recibe CATEGORIA_GESTIONAR en la semilla y es quien administra categorías
    // desde su panel. Exigir hasRole('ADMIN') lo dejaba fuera con un 403.
    @GetMapping("/todas")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCategoria>> listarTodasLasCategorias() {
        return ResponseEntity.ok(categoriaServicio.listarTodasLasCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaCategoria> obtenerCategoriaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaServicio.obtenerCategoriaPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCategoria> crearCategoria(@Valid @RequestBody PeticionCrearCategoria peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaServicio.crearCategoria(peticion));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCategoria> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarCategoria peticion) {
        return ResponseEntity.ok(categoriaServicio.actualizarCategoria(id, peticion));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarCategoria(@PathVariable Long id) {
        categoriaServicio.eliminarCategoria(id);
        return ResponseEntity.ok(new RespuestaMensaje("Categoria eliminada exitosamente"));
    }

    @GetMapping("/{id}/subcategorias")
    public ResponseEntity<List<RespuestaSubcategoria>> listarSubcategoriasPorCategoria(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaServicio.listarSubcategoriasPorCategoria(id));
    }
}
