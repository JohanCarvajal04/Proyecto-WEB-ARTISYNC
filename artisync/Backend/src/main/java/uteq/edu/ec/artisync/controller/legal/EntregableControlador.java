package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IEntregableServicio;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class EntregableControlador {

    private final IEntregableServicio entregableServicio;

    @PostMapping(value = "/{idPedido}/entregable", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaEntregable> subirEntregable(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("versionMarcaAgua") MultipartFile versionMarcaAgua,
            @RequestParam("versionLimpia") MultipartFile versionLimpia) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregableServicio.subirEntregable(idPedido, userDetails.getIdUsuario(),
                        versionMarcaAgua, versionLimpia));
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
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasAuthority('FONDOS_LIBERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> aprobarEntrega(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        entregableServicio.aprobarEntrega(idPedido, userDetails.getIdUsuario());
        return ResponseEntity.ok(new RespuestaMensaje("Entrega aprobada exitosamente. Fondos liberados."));
    }

    @GetMapping("/{idPedido}/entregable/descargar")
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> descargarVersionLimpia(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return responderArchivo(
                entregableServicio.descargarVersionLimpia(idPedido, userDetails.getIdUsuario()));
    }

    @GetMapping("/{idPedido}/entregable/descargar/marca-agua")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarVersionMarcaAgua(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return responderArchivo(
                entregableServicio.descargarVersionMarcaAgua(idPedido, userDetails.getIdUsuario()));
    }

    /**
     * Content-Disposition como attachment y no inline: un entregable puede ser
     * HTML o SVG, y servirlo para que el navegador lo interprete en el dominio
     * de la plataforma abriría la puerta a XSS almacenado.
     */
    private ResponseEntity<byte[]> responderArchivo(IEntregableServicio.ArchivoDescargado archivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreSugerido()).toString())
                .body(archivo.contenido());
    }
}
