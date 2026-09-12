package uteq.edu.ec.artisync.controller.catalogo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingSummaryResponse;
import uteq.edu.ec.artisync.service.catalogo.IOfferingCatalogService;

/**
 * SERVICIO_MODERAR estaba definido en las migraciones desde hace tiempo pero
 * no lo usaba ningún endpoint: un admin/moderador no tenía forma de corregir
 * la subcategoría de un servicio ajeno salvo pasando por el mismo formulario
 * del creador. Este controlador es esa pantalla de moderación.
 */
@RestController
@RequestMapping("/api/v1/admin/servicios")
@RequiredArgsConstructor
public class OfferingModerationController {

    private final IOfferingCatalogService servicioCatalogoServicio;

    /**
     * Lista los servicios para su revisión en el panel de moderación, con búsqueda de texto y paginación.
     *
     * @param texto texto de búsqueda opcional para filtrar servicios
     * @param page número de página solicitada (base 0)
     * @param size tamaño de la página
     * @return página con los servicios resumidos disponibles para moderación
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SERVICIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<OfferingSummaryResponse>> listForModeration(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(servicioCatalogoServicio.listForModeration(texto, page, size));
    }

    /**
     * Quita una subcategoría de un servicio ajeno como acción de moderación.
     *
     * @param idServicio identificador del servicio
     * @param idSubcategoria identificador de la subcategoría a quitar
     * @return el servicio actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el servicio quedaría sin subcategorías tras la operación
     */
    @DeleteMapping("/{idServicio}/subcategorias/{idSubcategoria}")
    @PreAuthorize("hasAuthority('SERVICIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<OfferingResponse> removeSubcategory(
            @PathVariable Long idServicio,
            @PathVariable Long idSubcategoria) {
        return ResponseEntity.ok(servicioCatalogoServicio.removeSubcategory(idServicio, idSubcategoria));
    }
}
