package uteq.edu.ec.artisync.controller.catalogo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingSummaryResponse;
import uteq.edu.ec.artisync.service.catalogo.IOfferingCatalogService;

import java.util.List;

/** Consulta pública de los servicios publicados por un creador. */
@RestController
@RequestMapping("/api/v1/creadores")
@RequiredArgsConstructor
public class CreatorOfferingController {

    private final IOfferingCatalogService servicioCatalogoServicio;

    /**
     * Lista los servicios publicados por un creador, opcionalmente filtrados por estado de publicación.
     *
     * @param idPerfilCreador identificador del perfil de creador
     * @param estadoPublicacion estado de publicación por el cual filtrar (opcional)
     * @return listado resumido de los servicios del creador
     */
    @GetMapping("/{idPerfilCreador}/servicios")
    public ResponseEntity<List<OfferingSummaryResponse>> listarServiciosPorCreador(
            @PathVariable Long idPerfilCreador,
            @RequestParam(required = false) String estadoPublicacion) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarServiciosPorCreador(idPerfilCreador, estadoPublicacion));
    }
}
