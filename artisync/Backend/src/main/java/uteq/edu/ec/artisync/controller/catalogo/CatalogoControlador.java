package uteq.edu.ec.artisync.controller.catalogo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
public class CatalogoControlador {

    private final IServicioCatalogoServicio servicioCatalogoServicio;

    @GetMapping
    public ResponseEntity<Page<RespuestaServicioResumido>> buscarCatalogo(
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) Long subcategoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) List<Long> etiquetas,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "idServicio,desc") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        return ResponseEntity.ok(servicioCatalogoServicio.buscarCatalogoServicios(
                categoria, subcategoria, precioMin, precioMax, etiquetas, q, sort, page, size));
    }
}
