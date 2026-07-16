package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaSeguimientoPedido;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IPedidoServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoControlador {

    private final IPedidoServicio pedidoServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<RespuestaPedido> crearPedido(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionCrearPedido peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoServicio.crearPedido(userDetails.getIdUsuario(), peticion));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPedido> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoServicio.obtenerPedidoPorId(id));
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
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

    @PutMapping("/{id}/avanzar")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaPedido> avanzarEtapa(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PeticionAvanzarEtapa peticion) {
        return ResponseEntity.ok(pedidoServicio.avanzarEtapa(id, userDetails.getIdUsuario(), peticion));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RespuestaHistorialEstado>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoServicio.obtenerHistorial(id));
    }

    @GetMapping("/{id}/seguimiento")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaSeguimientoPedido> obtenerSeguimiento(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoServicio.obtenerSeguimiento(id));
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
