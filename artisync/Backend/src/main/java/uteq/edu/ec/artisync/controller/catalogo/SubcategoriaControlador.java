package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaSubcategoria> crearSubcategoria(@Valid @RequestBody PeticionCrearSubcategoria peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaServicio.crearSubcategoria(peticion));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarSubcategoria(@PathVariable Long id) {
        categoriaServicio.eliminarSubcategoria(id);
        return ResponseEntity.ok(new RespuestaMensaje("Subcategoria eliminada exitosamente"));
    }
}
