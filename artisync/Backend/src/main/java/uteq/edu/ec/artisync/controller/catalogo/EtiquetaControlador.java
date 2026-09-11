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

    /**
     * Lista todas las etiquetas disponibles en el catálogo.
     *
     * @return listado de etiquetas
     */
    @GetMapping
    public ResponseEntity<List<RespuestaEtiqueta>> listarEtiquetas() {
        return ResponseEntity.ok(etiquetaServicio.listarEtiquetas());
    }

    /**
     * Obtiene una etiqueta por su identificador.
     *
     * @param id identificador de la etiqueta
     * @return la etiqueta solicitada
     * @throws ResourceNotFoundException si la etiqueta no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<RespuestaEtiqueta> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(etiquetaServicio.obtenerPorId(id));
    }

    /**
     * Crea una nueva etiqueta en el catálogo.
     *
     * @param peticion datos de la etiqueta a crear
     * @return la etiqueta creada, con estado 201
     * @throws BusinessRuleException si ya existe una etiqueta con el mismo nombre
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaEtiqueta> crearEtiqueta(@Valid @RequestBody PeticionCrearEtiqueta peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etiquetaServicio.crearEtiqueta(peticion));
    }

    // Mismo criterio que CategoriaControlador: MODERADOR administra el
    // catálogo completo (categorías, subcategorías y etiquetas) vía el
    // permiso CATEGORIA_GESTIONAR, no vía ROLE_ADMIN.
    /**
     * Elimina una etiqueta del catálogo.
     *
     * @param id identificador de la etiqueta a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws ResourceNotFoundException si la etiqueta no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarEtiqueta(@PathVariable Long id) {
        etiquetaServicio.eliminarEtiqueta(id);
        return ResponseEntity.ok(new RespuestaMensaje("Etiqueta eliminada exitosamente"));
    }
}
