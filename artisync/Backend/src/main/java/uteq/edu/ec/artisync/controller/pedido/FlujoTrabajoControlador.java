package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;
import uteq.edu.ec.artisync.service.pedido.IFlujoTrabajoServicio;

import java.util.List;

/**
 * Desde V25 (flujos_por_creador) cada FlujoTrabajo es propiedad de un
 * creador. FLUJO_GESTIONAR es autoservicio: gestiona los flujos propios.
 * FLUJO_MODERAR (V26) es supervisión: ve y gestiona los de todos los
 * creadores — lo necesita, por ejemplo, el selector de flujo en Categorías,
 * que no tiene sentido acotado a los flujos de un solo usuario.
 */
@RestController
@RequestMapping("/api/v1/flujos")
@RequiredArgsConstructor
public class FlujoTrabajoControlador {

    private final IFlujoTrabajoServicio flujoTrabajoServicio;

    /**
     * Crea un nuevo flujo de trabajo para el usuario autenticado.
     *
     * @param peticion datos del flujo de trabajo a crear, incluyendo sus etapas
     * @param userDetails usuario autenticado propietario del flujo
     * @return el flujo de trabajo creado, con estado 201
     * @throws DuplicateResourceException si ya existe un flujo con el mismo nombre
     * @throws ResourceNotFoundException si el usuario no existe
     * @throws BusinessRuleException si las etapas indicadas tienen nombres o números de orden repetidos
     */
    @PostMapping
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> crearFlujo(
            @Valid @RequestBody PeticionCrearFlujoTrabajo peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flujoTrabajoServicio.crearFlujoTrabajo(userDetails.getIdUsuario(), peticion));
    }

    /**
     * Lista los flujos de trabajo visibles para el usuario autenticado: los propios,
     * o todos si tiene FLUJO_MODERAR/ADMIN.
     *
     * @param userDetails usuario autenticado
     * @return listado de flujos de trabajo
     */
    @GetMapping
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaFlujoTrabajo>> listarFlujos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.listarFlujosTrabajo(
                userDetails.getIdUsuario(), puedeVerTodos(userDetails)));
    }

    /**
     * Obtiene el detalle de un flujo de trabajo por su identificador.
     *
     * @param id identificador del flujo de trabajo
     * @param userDetails usuario autenticado que consulta el flujo
     * @return el flujo de trabajo solicitado
     * @throws ResourceNotFoundException si el flujo no existe o no es accesible para el usuario
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> obtenerFlujo(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.obtenerFlujoPorId(
                id, userDetails.getIdUsuario(), puedeVerTodos(userDetails)));
    }

    /**
     * Actualiza los datos de un flujo de trabajo existente.
     *
     * @param id identificador del flujo de trabajo a actualizar
     * @param peticion datos actualizados del flujo de trabajo
     * @param userDetails usuario autenticado que solicita la actualización
     * @return el flujo de trabajo actualizado
     * @throws ResourceNotFoundException si el flujo no existe o no es accesible para el usuario
     * @throws DuplicateResourceException si ya existe otro flujo con el mismo nombre
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> actualizarFlujo(
            @PathVariable Long id,
            @Valid @RequestBody PeticionCrearFlujoTrabajo peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.actualizarFlujoTrabajo(
                id, userDetails.getIdUsuario(), puedeVerTodos(userDetails), peticion));
    }

    /**
     * Agrega una nueva etapa a un flujo de trabajo.
     *
     * @param id identificador del flujo de trabajo
     * @param peticion datos de la etapa a agregar
     * @param userDetails usuario autenticado que solicita agregar la etapa
     * @return el flujo de trabajo actualizado, con estado 201
     * @throws ResourceNotFoundException si el flujo no existe o no es accesible para el usuario
     * @throws DuplicateResourceException si ya existe una etapa con el mismo nombre en el flujo
     * @throws BusinessRuleException si el número de orden de la etapa ya está en uso
     */
    @PostMapping("/{id}/etapas")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> agregarEtapa(
            @PathVariable Long id,
            @Valid @RequestBody PeticionEtapaConfig peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flujoTrabajoServicio.agregarEtapa(
                        id, userDetails.getIdUsuario(), puedeVerTodos(userDetails), peticion));
    }

    /**
     * Actualiza la configuración de una etapa de un flujo de trabajo.
     *
     * @param id identificador del flujo de trabajo
     * @param etapaId identificador de la configuración de etapa a actualizar
     * @param peticion datos actualizados de la etapa
     * @param userDetails usuario autenticado que solicita la actualización
     * @return el flujo de trabajo actualizado
     * @throws ResourceNotFoundException si el flujo o la configuración de etapa no existen
     * @throws BusinessRuleException si la etapa no pertenece al flujo especificado
     */
    @PutMapping("/{id}/etapas/{etapaId}")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> actualizarEtapa(
            @PathVariable Long id,
            @PathVariable Long etapaId,
            @Valid @RequestBody PeticionEtapaConfig peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.actualizarEtapa(
                id, etapaId, userDetails.getIdUsuario(), puedeVerTodos(userDetails), peticion));
    }

    /**
     * Intercambia el orden de dos etapas de un flujo de trabajo.
     *
     * @param id identificador del flujo de trabajo
     * @param peticion identificadores de las dos etapas a intercambiar
     * @param userDetails usuario autenticado que solicita el intercambio
     * @return el flujo de trabajo actualizado
     * @throws ResourceNotFoundException si el flujo o alguna de las configuraciones de etapa no existen
     * @throws BusinessRuleException si se intenta intercambiar una etapa consigo misma, o las etapas no pertenecen al flujo
     */
    @PutMapping("/{id}/etapas/reordenar")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> intercambiarOrdenEtapas(
            @PathVariable Long id,
            @Valid @RequestBody PeticionSwapEtapas peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.intercambiarOrdenEtapas(
                id, userDetails.getIdUsuario(), puedeVerTodos(userDetails), peticion));
    }

    /**
     * Elimina una etapa de un flujo de trabajo.
     *
     * @param id identificador del flujo de trabajo
     * @param etapaId identificador de la configuración de etapa a eliminar
     * @param userDetails usuario autenticado que solicita la eliminación
     * @return mensaje de confirmación de la eliminación
     * @throws ResourceNotFoundException si el flujo o la configuración de etapa no existen
     * @throws BusinessRuleException si la etapa no pertenece al flujo o hay pedidos actualmente detenidos en ella
     */
    @DeleteMapping("/{id}/etapas/{etapaId}")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarEtapa(
            @PathVariable Long id,
            @PathVariable Long etapaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        flujoTrabajoServicio.eliminarEtapa(id, etapaId, userDetails.getIdUsuario(), puedeVerTodos(userDetails));
        return ResponseEntity.ok(new RespuestaMensaje("Etapa eliminada exitosamente del flujo de trabajo"));
    }

    private boolean puedeVerTodos(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("FLUJO_MODERAR") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
