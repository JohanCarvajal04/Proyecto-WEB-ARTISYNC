package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateTagRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.TagResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.ITagService;

import java.util.List;

/** Consulta y gestión de las etiquetas del catálogo de servicios. */
@RestController
@RequestMapping("/api/v1/etiquetas")
@RequiredArgsConstructor
public class TagController {

    private final ITagService etiquetaServicio;

    /**
     * Lista todas las etiquetas disponibles en el catálogo.
     *
     * @return listado de etiquetas
     */
    @GetMapping
    public ResponseEntity<List<TagResponse>> listTags() {
        return ResponseEntity.ok(etiquetaServicio.listTags());
    }

    /**
     * Obtiene una etiqueta por su identificador.
     *
     * @param id identificador de la etiqueta
     * @return la etiqueta solicitada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la etiqueta no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(etiquetaServicio.getById(id));
    }

    /**
     * Crea una nueva etiqueta en el catálogo.
     *
     * @param peticion datos de la etiqueta a crear
     * @return la etiqueta creada, con estado 201
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una etiqueta con el mismo nombre
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etiquetaServicio.createTag(peticion));
    }

    // Mismo criterio que CategoryController: MODERADOR administra el
    // catálogo completo (categorías, subcategorías y etiquetas) vía el
    // permiso CATEGORIA_GESTIONAR, no vía ROLE_ADMIN.
    /**
     * Elimina una etiqueta del catálogo.
     *
     * @param id identificador de la etiqueta a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la etiqueta no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> deleteTag(@PathVariable Long id) {
        etiquetaServicio.deleteTag(id);
        return ResponseEntity.ok(new RespuestaMensaje("Tag eliminada exitosamente"));
    }
}
