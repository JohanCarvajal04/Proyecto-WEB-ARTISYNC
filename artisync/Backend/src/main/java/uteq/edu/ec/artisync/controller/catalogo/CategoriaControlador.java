package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaControlador {

    private final ICategoriaServicio categoriaServicio;

    /**
     * Lista las categorías activas y ya revisadas, visibles públicamente en
     * el catálogo.
     * @return listado de categorías activas
     */
    @GetMapping
    public ResponseEntity<List<RespuestaCategoria>> listarCategoriasActivas() {
        return ResponseEntity.ok(categoriaServicio.listarCategoriasActivas());
    }

    // La gestión del catálogo se autoriza por permiso, no por rol: MODERADOR
    // recibe CATEGORIA_GESTIONAR en la semilla y es quien administra categorías
    // desde su panel. Exigir hasRole('ADMIN') lo dejaba fuera con un 403.
    /**
     * Lista absolutamente todas las categorías, incluidas las inactivas y las
     * pendientes de revisión, para el panel de administración del catálogo.
     * @return listado completo de categorías sin filtrar por estado
     */
    @GetMapping("/todas")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCategoria>> listarTodasLasCategorias() {
        return ResponseEntity.ok(categoriaServicio.listarTodasLasCategorias());
    }

    /**
     * Obtiene el detalle de una categoría puntual del catálogo.
     * @param id identificador de la categoría
     * @return datos de la categoría solicitada
     */
    @GetMapping("/{id}")
    public ResponseEntity<RespuestaCategoria> obtenerCategoriaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaServicio.obtenerCategoriaPorId(id));
    }

    // CATEGORIA_CREAR es autoservicio (cada creador crea las suyas, quedan sin
    // revisar); CATEGORIA_GESTIONAR/ADMIN crean directamente como revisadas,
    // igual que siempre.
    /**
     * Crea una categoría nueva; si quien la crea es un creador con
     * CATEGORIA_CREAR queda asociada a él y sin revisar, mientras que un
     * moderador o administrador la crea ya como revisada.
     * @param peticion datos de la categoría a crear
     * @param userDetails usuario autenticado que solicita la creación
     * @return categoría recién creada
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasAuthority('CATEGORIA_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCategoria> crearCategoria(
            @Valid @RequestBody PeticionCrearCategoria peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioCreador = esModerador(userDetails) ? null : userDetails.getIdUsuario();
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaServicio.crearCategoria(idUsuarioCreador, peticion));
    }

    /**
     * Actualiza los datos de una categoría existente.
     * @param id identificador de la categoría a modificar
     * @param peticion nuevos datos a aplicar sobre la categoría
     * @return categoría con los cambios aplicados
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCategoria> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarCategoria peticion) {
        return ResponseEntity.ok(categoriaServicio.actualizarCategoria(id, peticion));
    }

    /**
     * Elimina (desactiva) una categoría del catálogo, opcionalmente
     * registrando el motivo de la baja.
     * @param id identificador de la categoría a eliminar
     * @param motivo justificación de la eliminación, si se provee
     * @return mensaje de confirmación de la eliminación
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarCategoria(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        categoriaServicio.eliminarCategoria(id, motivo);
        return ResponseEntity.ok(new RespuestaMensaje("Categoria eliminada exitosamente"));
    }

    /**
     * Lista las subcategorías que pertenecen a una categoría concreta.
     * @param id identificador de la categoría padre
     * @return listado de subcategorías asociadas a la categoría
     */
    @GetMapping("/{id}/subcategorias")
    public ResponseEntity<List<RespuestaSubcategoria>> listarSubcategoriasPorCategoria(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaServicio.listarSubcategoriasPorCategoria(id));
    }

    /**
     * Lista las categorías creadas por autoservicio que aún no han sido
     * revisadas por un moderador o administrador.
     * @return listado de categorías pendientes de revisión
     */
    @GetMapping("/pendientes-revision")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCategoria>> listarPendientesRevision() {
        return ResponseEntity.ok(categoriaServicio.listarCategoriasPendientesRevision());
    }

    /**
     * Marca una categoría pendiente como revisada por un moderador o
     * administrador.
     * @param id identificador de la categoría a marcar como revisada
     * @return categoría actualizada con el estado de revisión aplicado
     */
    @PatchMapping("/{id}/revisar")
    @PreAuthorize("hasAuthority('CATEGORIA_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCategoria> marcarRevisada(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaServicio.marcarCategoriaRevisada(id));
    }

    private boolean esModerador(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CATEGORIA_GESTIONAR") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
