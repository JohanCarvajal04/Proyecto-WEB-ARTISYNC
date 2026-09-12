package uteq.edu.ec.artisync.controller.pedido;

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
import uteq.edu.ec.artisync.dto.respuesta.pedido.SketchResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.ISketchService;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class SketchController {

    private final ISketchService bocetoServicio;

    /**
     * Sube (o resube, reemplazando el anterior) el boceto de un pedido.
     *
     * @param idPedido    identificador del pedido
     * @param userDetails usuario autenticado que sube el boceto
     * @param imagen      imagen con marca de agua ya aplicada por el creador
     * @return el boceto guardado, con estado 201
     */
    @PostMapping(value = "/{idPedido}/boceto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<SketchResponse> subirBoceto(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("imagen") MultipartFile imagen) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bocetoServicio.subirBoceto(idPedido, userDetails.getIdUsuario(), imagen));
    }

    /**
     * Obtiene el boceto vigente de un pedido.
     *
     * @param idPedido    identificador del pedido
     * @param userDetails usuario autenticado que consulta el boceto
     * @return el boceto del pedido
     */
    @GetMapping("/{idPedido}/boceto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SketchResponse> obtenerBoceto(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(bocetoServicio.obtenerBoceto(idPedido, userDetails.getIdUsuario()));
    }

    /**
     * Descarga la imagen del boceto vigente de un pedido.
     *
     * @param idPedido    identificador del pedido
     * @param userDetails usuario autenticado que solicita la descarga
     * @return el contenido binario de la imagen, como adjunto
     */
    @GetMapping("/{idPedido}/boceto/descargar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarBoceto(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ISketchService.ArchivoDescargado archivo =
                bocetoServicio.descargarBoceto(idPedido, userDetails.getIdUsuario());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreSugerido()).build().toString())
                .body(archivo.contenido());
    }
}
