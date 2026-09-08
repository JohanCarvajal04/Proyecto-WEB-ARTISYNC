package uteq.edu.ec.artisync.controller.catalogo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/creadores")
@RequiredArgsConstructor
public class CreadorServicioControlador {

    private final IServicioCatalogoServicio servicioCatalogoServicio;

    /**
     * Lista los servicios publicados por un creador, opcionalmente filtrados por estado de publicación.
     *
     * @param idPerfilCreador identificador del perfil de creador
     * @param estadoPublicacion estado de publicación por el cual filtrar (opcional)
     * @return listado resumido de los servicios del creador
     */
    @GetMapping("/{idPerfilCreador}/servicios")
    public ResponseEntity<List<RespuestaServicioResumido>> listarServiciosPorCreador(
            @PathVariable Long idPerfilCreador,
            @RequestParam(required = false) String estadoPublicacion) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarServiciosPorCreador(idPerfilCreador, estadoPublicacion));
    }
}
