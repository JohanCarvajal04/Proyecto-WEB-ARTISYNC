package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IEntregableServicio;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class EntregableControlador {

    private final IEntregableServicio entregableServicio;

    @PostMapping("/{idPedido}/entregable")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaEntregable> subirEntregable(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String urlMarcaAgua,
            @RequestParam String urlLimpia) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregableServicio.subirEntregable(idPedido, userDetails.getIdUsuario(),
                        urlMarcaAgua, urlLimpia));
    }

    @GetMapping("/{idPedido}/entregable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaEntregable> obtenerEntregable(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                entregableServicio.obtenerEntregable(idPedido, userDetails.getIdUsuario()));
    }

    @PostMapping("/{idPedido}/aprobar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<RespuestaMensaje> aprobarEntrega(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        entregableServicio.aprobarEntrega(idPedido, userDetails.getIdUsuario());
        return ResponseEntity.ok(new RespuestaMensaje("Entrega aprobada exitosamente. Fondos liberados."));
    }

    @GetMapping("/{idPedido}/entregable/descargar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<byte[]> descargarVersionLimpia(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        byte[] archivo = entregableServicio.descargarVersionLimpia(idPedido, userDetails.getIdUsuario());
        return ResponseEntity.ok(archivo);
    }
}
