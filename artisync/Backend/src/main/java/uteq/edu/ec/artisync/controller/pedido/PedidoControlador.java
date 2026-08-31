package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaSeguimientoPedido;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IPedidoServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.RespuestaDocumento;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoControlador {

    private final IPedidoServicio pedidoServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPedido> crearPedido(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionCrearPedido peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoServicio.crearPedido(userDetails.getIdUsuario(), peticion));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPedido> obtenerPedido(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.obtenerPedidoPorId(id, userDetails.getIdUsuario()));
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CREADOR', 'ADMIN')")
    public ResponseEntity<List<RespuestaPedidoResumido>> listarMisPedidos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.listarMisPedidos(userDetails.getIdUsuario()));
    }

    @GetMapping("/mis-comisiones")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<List<RespuestaPedidoResumido>> listarMisComisiones(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.listarMisComisiones(userDetails.getIdUsuario()));
    }

    /**
     * Exportación "propia": mismo @PreAuthorize que el listado, sin permiso
     * de exportación aparte — a diferencia de auditoría/finanzas/contratos,
     * que son reportes administrativos y sí lo llevan.
     */
    @GetMapping("/mis-pedidos/exportar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CREADOR', 'ADMIN')")
    public ResponseEntity<byte[]> exportarMisPedidos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam FormatoReporte formato,
            Authentication authentication) {
        DocumentoGenerado documento = pedidoServicio.exportarMisPedidos(
                userDetails.getIdUsuario(), formato, authentication.getName());
        return RespuestaDocumento.de(documento);
    }

    @GetMapping("/mis-comisiones/exportar")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<byte[]> exportarMisComisiones(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam FormatoReporte formato,
            @RequestParam(required = false) List<Long> idsPedido,
            Authentication authentication) {
        DocumentoGenerado documento = pedidoServicio.exportarMisComisiones(
                userDetails.getIdUsuario(), idsPedido, formato, authentication.getName());
        return RespuestaDocumento.de(documento);
    }

    @PutMapping("/{id}/avanzar")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPedido> avanzarEtapa(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PeticionAvanzarEtapa peticion) {
        return ResponseEntity.ok(pedidoServicio.avanzarEtapa(id, userDetails.getIdUsuario(), peticion));
    }

    @PostMapping("/{id}/propuestas-terminos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPropuestaTerminos> proponerTerminos(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionCrearPropuestaTerminos peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoServicio.proponerTerminos(id, userDetails.getIdUsuario(), peticion));
    }

    @GetMapping("/{id}/propuestas-terminos/pendiente")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPropuestaTerminos> obtenerPropuestaPendiente(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.obtenerPropuestaPendiente(id, userDetails.getIdUsuario()));
    }

    @PutMapping("/{id}/propuestas-terminos/{idPropuesta}/aceptar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPedido> aceptarPropuestaTerminos(
            @PathVariable Long id,
            @PathVariable Long idPropuesta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.aceptarPropuestaTerminos(id, idPropuesta, userDetails.getIdUsuario()));
    }

    @PutMapping("/{id}/propuestas-terminos/{idPropuesta}/rechazar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPropuestaTerminos> rechazarPropuestaTerminos(
            @PathVariable Long id,
            @PathVariable Long idPropuesta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.rechazarPropuestaTerminos(id, idPropuesta, userDetails.getIdUsuario()));
    }

    @PutMapping("/{id}/propuestas-terminos/{idPropuesta}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPropuestaTerminos> cancelarPropuestaTerminos(
            @PathVariable Long id,
            @PathVariable Long idPropuesta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.cancelarPropuestaTerminos(id, idPropuesta, userDetails.getIdUsuario()));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RespuestaHistorialEstado>> obtenerHistorial(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.obtenerHistorial(id, userDetails.getIdUsuario()));
    }

    @GetMapping("/{id}/seguimiento")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaSeguimientoPedido> obtenerSeguimiento(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.obtenerSeguimiento(id, userDetails.getIdUsuario()));
    }

    // ── Inmutabilidad del Historial (RNF-13) ─────────────────────────────────
    // Los registros de historial_estados_pedido NO pueden ser eliminados ni modificados

    @DeleteMapping("/{id}/historial")
    public ResponseEntity<RespuestaMensaje> bloquearDeleteHistorial(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RespuestaMensaje("Operacion no permitida sobre registros de auditoria"));
    }

    @PatchMapping("/{id}/historial")
    public ResponseEntity<RespuestaMensaje> bloquearPatchHistorial(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RespuestaMensaje("Operacion no permitida sobre registros de auditoria"));
    }
}
