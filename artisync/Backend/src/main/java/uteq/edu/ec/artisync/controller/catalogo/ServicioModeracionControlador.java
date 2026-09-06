package uteq.edu.ec.artisync.controller.catalogo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;

/**
 * SERVICIO_MODERAR estaba definido en las migraciones desde hace tiempo pero
 * no lo usaba ningún endpoint: un admin/moderador no tenía forma de corregir
 * la subcategoría de un servicio ajeno salvo pasando por el mismo formulario
 * del creador. Este controlador es esa pantalla de moderación.
 */
@RestController
@RequestMapping("/api/v1/admin/servicios")
@RequiredArgsConstructor
public class ServicioModeracionControlador {

    private final IServicioCatalogoServicio servicioCatalogoServicio;

    @GetMapping
    @PreAuthorize("hasAuthority('SERVICIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<RespuestaServicioResumido>> listarParaModeracion(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarParaModeracion(texto, page, size));
    }

    @DeleteMapping("/{idServicio}/subcategorias/{idSubcategoria}")
    @PreAuthorize("hasAuthority('SERVICIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaServicio> quitarSubcategoria(
            @PathVariable Long idServicio,
            @PathVariable Long idSubcategoria) {
        return ResponseEntity.ok(servicioCatalogoServicio.quitarSubcategoria(idServicio, idSubcategoria));
    }
}
