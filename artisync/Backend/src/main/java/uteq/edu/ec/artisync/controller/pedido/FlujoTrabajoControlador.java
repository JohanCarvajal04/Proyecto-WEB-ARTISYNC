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

    @PostMapping
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> crearFlujo(
            @Valid @RequestBody PeticionCrearFlujoTrabajo peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flujoTrabajoServicio.crearFlujoTrabajo(userDetails.getIdUsuario(), peticion));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaFlujoTrabajo>> listarFlujos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.listarFlujosTrabajo(
                userDetails.getIdUsuario(), puedeVerTodos(userDetails)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> obtenerFlujo(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.obtenerFlujoPorId(
                id, userDetails.getIdUsuario(), puedeVerTodos(userDetails)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> actualizarFlujo(
            @PathVariable Long id,
            @Valid @RequestBody PeticionCrearFlujoTrabajo peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.actualizarFlujoTrabajo(
                id, userDetails.getIdUsuario(), puedeVerTodos(userDetails), peticion));
    }

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

    @PutMapping("/{id}/etapas/reordenar")
    @PreAuthorize("hasAuthority('FLUJO_GESTIONAR') or hasAuthority('FLUJO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaFlujoTrabajo> intercambiarOrdenEtapas(
            @PathVariable Long id,
            @Valid @RequestBody PeticionSwapEtapas peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(flujoTrabajoServicio.intercambiarOrdenEtapas(
                id, userDetails.getIdUsuario(), puedeVerTodos(userDetails), peticion));
    }

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
