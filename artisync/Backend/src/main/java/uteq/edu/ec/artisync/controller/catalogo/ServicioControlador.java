package uteq.edu.ec.artisync.controller.catalogo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarServicio;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaAtributo;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaUrl;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.almacenamiento.ExtensionesArchivo;
import uteq.edu.ec.artisync.service.shared.almacenamiento.PrefijoAlmacenamiento;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioControlador {

    private final IServicioCatalogoServicio servicioCatalogoServicio;
    private final AlmacenamientoDocumentos almacenamientoDocumentos;

    @PostMapping("/creador/{idPerfilCreador}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaServicio> crearServicio(
            @PathVariable Long idPerfilCreador,
            @Valid @RequestBody PeticionCrearServicio peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.crearServicio(idPerfilCreador, peticion));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaServicio> actualizarServicio(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarServicio peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarServicio(id, peticion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaServicio> obtenerServicioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.obtenerServicioPorId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarServicio(@PathVariable Long id) {
        servicioCatalogoServicio.eliminarServicio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Servicio eliminado exitosamente"));
    }

    @GetMapping("/creador/{idPerfilCreador}")
    public ResponseEntity<List<RespuestaServicioResumido>> listarServiciosPorCreador(
            @PathVariable Long idPerfilCreador,
            @RequestParam(required = false) String estadoPublicacion) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarServiciosPorCreador(idPerfilCreador, estadoPublicacion));
    }

    @GetMapping("/{id}/atributos")
    public ResponseEntity<List<RespuestaAtributo>> listarAtributosPorServicio(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarAtributosPorServicio(id));
    }

    @PostMapping("/{id}/atributos")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaAtributo> agregarAtributo(
            @PathVariable Long id,
            @Valid @RequestBody PeticionCrearAtributo peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.agregarAtributo(id, peticion));
    }

    @PutMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaAtributo> actualizarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo,
            @Valid @RequestBody PeticionActualizarAtributo peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarAtributo(id, idAtributo, peticion));
    }

    @DeleteMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo) {
        servicioCatalogoServicio.eliminarAtributo(id, idAtributo);
        return ResponseEntity.ok(new RespuestaMensaje("Atributo eliminado exitosamente del servicio"));
    }

    @PostMapping(value = "/miniatura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaUrl> subirMiniatura(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RespuestaUrl(servicioCatalogoServicio.subirMiniatura(archivo)));
    }

    /**
     * Sirve la miniatura públicamente (sin autenticación): se muestra en el
     * catálogo a cualquier visitante. Mismo criterio que
     * UserController.servirFotoPerfil: solo referencias bajo "servicios/" son
     * válidas aquí, para no convertir esto en una puerta trasera a otros
     * prefijos (verificacion, entregables) que sí son privados.
     */
    @GetMapping("/miniatura/**")
    public ResponseEntity<byte[]> servirMiniatura(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/v1/servicios/miniatura/";
        String referencia = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
        if (!referencia.startsWith(PrefijoAlmacenamiento.SERVICIOS + "/")) {
            throw new ExcepcionRecursoNoEncontrado("Miniatura no disponible: " + referencia);
        }
        byte[] contenido = almacenamientoDocumentos.leer(referencia);
        String contentType = ExtensionesArchivo.contentTypeDe(referencia);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + TimeUnit.DAYS.toSeconds(7))
                .body(contenido);
    }
}
