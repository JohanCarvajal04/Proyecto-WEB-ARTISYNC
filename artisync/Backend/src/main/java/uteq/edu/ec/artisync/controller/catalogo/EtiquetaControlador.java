package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.IEtiquetaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/etiquetas")
@RequiredArgsConstructor
public class EtiquetaControlador {

    private final IEtiquetaServicio etiquetaServicio;

    @GetMapping
    public ResponseEntity<List<RespuestaEtiqueta>> listarEtiquetas() {
        return ResponseEntity.ok(etiquetaServicio.listarEtiquetas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaEtiqueta> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(etiquetaServicio.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaEtiqueta> crearEtiqueta(@Valid @RequestBody PeticionCrearEtiqueta peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etiquetaServicio.crearEtiqueta(peticion));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarEtiqueta(@PathVariable Long id) {
        etiquetaServicio.eliminarEtiqueta(id);
        return ResponseEntity.ok(new RespuestaMensaje("Etiqueta eliminada exitosamente"));
    }
}
