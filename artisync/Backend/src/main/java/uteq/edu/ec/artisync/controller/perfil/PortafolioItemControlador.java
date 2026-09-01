package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
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
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolioItem;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPortafolioItemServicio;

import java.util.List;

/**
 * Obras del portafolio. Las rutas de lectura cuelgan de /api/v1/portafolios,
 * que SecurityConfig deja abierto en GET: un portafolio público debe poder
 * verse sin sesión, y el servicio es quien filtra los privados.
 */
@RestController
@RequestMapping("/api/v1/portafolios")
@RequiredArgsConstructor
public class PortafolioItemControlador {

    private final IPortafolioItemServicio itemServicio;

    @PostMapping(value = "/{idPortafolio}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaPortafolioItem> subirItem(
            @PathVariable Long idPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestPart("datos") PeticionCrearPortafolioItem datos,
            @RequestPart("archivo") MultipartFile archivo) {
        RespuestaPortafolioItem respuesta = itemServicio.subirItem(
                idPortafolio, userDetails.getIdUsuario(), datos, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{idPortafolio}/items")
    public ResponseEntity<List<RespuestaPortafolioItem>> listarItems(
            @PathVariable Long idPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(itemServicio.listarItems(idPortafolio, idDe(userDetails)));
    }

    @GetMapping("/items/{idItem}")
    public ResponseEntity<RespuestaPortafolioItem> obtenerItem(
            @PathVariable Long idItem,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(itemServicio.obtenerItem(idItem, idDe(userDetails)));
    }

    /**
     * Sirve los bytes cuando el proveedor no firma URLs. Con Azure el frontend
     * recibe un SAS en la respuesta y no pasa por aquí.
     *
     * <p>Content-Disposition attachment y no inline: una obra puede ser un SVG,
     * y servirlo para que el navegador lo interprete en el dominio de la
     * plataforma abriría la puerta a XSS almacenado.
     */
    @GetMapping("/items/{idItem}/archivo")
    public ResponseEntity<byte[]> descargarArchivo(
            @PathVariable Long idItem,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        IPortafolioItemServicio.ArchivoItem archivo =
                itemServicio.descargarArchivo(idItem, idDe(userDetails));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreSugerido()).toString())
                .body(archivo.contenido());
    }

    @PutMapping("/items/{idItem}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaPortafolioItem> actualizarItem(
            @PathVariable Long idItem,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionCrearPortafolioItem datos) {
        return ResponseEntity.ok(itemServicio.actualizarItem(idItem, userDetails.getIdUsuario(), datos));
    }

    @DeleteMapping("/items/{idItem}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarItem(
            @PathVariable Long idItem,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        itemServicio.eliminarItem(idItem, userDetails.getIdUsuario());
        return ResponseEntity.ok(new RespuestaMensaje("Obra eliminada del portafolio"));
    }

    /** Nulo cuando la petición es anónima, que en GET es un caso válido. */
    private Long idDe(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getIdUsuario();
    }
}
