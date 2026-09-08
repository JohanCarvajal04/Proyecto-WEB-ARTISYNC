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

    /**
     * Crea un nuevo servicio para el perfil de creador indicado.
     *
     * @param idPerfilCreador identificador del perfil de creador propietario del servicio
     * @param peticion datos del servicio a crear
     * @return el servicio creado, con estado 201
     * @throws ExcepcionReglaNegocio si el precio es menor a 0.01 USD
     * @throws ExcepcionRecursoNoEncontrado si el perfil de creador no existe
     */
    @PostMapping("/creador/{idPerfilCreador}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaServicio> crearServicio(
            @PathVariable Long idPerfilCreador,
            @Valid @RequestBody PeticionCrearServicio peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.crearServicio(idPerfilCreador, peticion));
    }

    /**
     * Actualiza los datos de un servicio existente.
     *
     * @param id identificador del servicio a actualizar
     * @param peticion datos actualizados del servicio
     * @return el servicio actualizado
     * @throws ExcepcionReglaNegocio si el precio es menor a 0.01 USD o si el servicio queda sin subcategorías
     * @throws ExcepcionRecursoNoEncontrado si el servicio no existe
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaServicio> actualizarServicio(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarServicio peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarServicio(id, peticion));
    }

    /**
     * Obtiene el detalle de un servicio por su identificador.
     *
     * @param id identificador del servicio
     * @return el servicio solicitado
     * @throws ExcepcionRecursoNoEncontrado si el servicio no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<RespuestaServicio> obtenerServicioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.obtenerServicioPorId(id));
    }

    /**
     * Elimina un servicio existente.
     *
     * @param id identificador del servicio a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws ExcepcionRecursoNoEncontrado si el servicio no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarServicio(@PathVariable Long id) {
        servicioCatalogoServicio.eliminarServicio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Servicio eliminado exitosamente"));
    }

    /**
     * Lista los servicios de un creador, opcionalmente filtrados por estado de publicación.
     *
     * @param idPerfilCreador identificador del perfil de creador
     * @param estadoPublicacion estado de publicación por el cual filtrar (opcional)
     * @return listado resumido de los servicios del creador
     * @throws ExcepcionRecursoNoEncontrado si el perfil de creador no existe
     */
    @GetMapping("/creador/{idPerfilCreador}")
    public ResponseEntity<List<RespuestaServicioResumido>> listarServiciosPorCreador(
            @PathVariable Long idPerfilCreador,
            @RequestParam(required = false) String estadoPublicacion) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarServiciosPorCreador(idPerfilCreador, estadoPublicacion));
    }

    /**
     * Lista los atributos personalizados asociados a un servicio.
     *
     * @param id identificador del servicio
     * @return listado de atributos del servicio
     * @throws ExcepcionRecursoNoEncontrado si el servicio no existe
     */
    @GetMapping("/{id}/atributos")
    public ResponseEntity<List<RespuestaAtributo>> listarAtributosPorServicio(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarAtributosPorServicio(id));
    }

    /**
     * Agrega un atributo personalizado a un servicio.
     *
     * @param id identificador del servicio
     * @param peticion datos del atributo a agregar
     * @return el atributo creado, con estado 201
     * @throws ExcepcionRecursoNoEncontrado si el servicio no existe
     * @throws ExcepcionReglaNegocio si se alcanzó el límite de atributos permitidos o el atributo ya está asociado al servicio
     */
    @PostMapping("/{id}/atributos")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaAtributo> agregarAtributo(
            @PathVariable Long id,
            @Valid @RequestBody PeticionCrearAtributo peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.agregarAtributo(id, peticion));
    }

    /**
     * Actualiza un atributo personalizado de un servicio.
     *
     * @param id identificador del servicio
     * @param idAtributo identificador del atributo a actualizar
     * @param peticion datos actualizados del atributo
     * @return el atributo actualizado
     * @throws ExcepcionRecursoNoEncontrado si el servicio o el atributo no existen
     * @throws ExcepcionReglaNegocio si el atributo no pertenece al servicio indicado
     */
    @PutMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaAtributo> actualizarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo,
            @Valid @RequestBody PeticionActualizarAtributo peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarAtributo(id, idAtributo, peticion));
    }

    /**
     * Elimina un atributo personalizado de un servicio.
     *
     * @param id identificador del servicio
     * @param idAtributo identificador del atributo a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws ExcepcionRecursoNoEncontrado si el servicio o el atributo no existen
     * @throws ExcepcionReglaNegocio si el atributo no pertenece al servicio indicado
     */
    @DeleteMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo) {
        servicioCatalogoServicio.eliminarAtributo(id, idAtributo);
        return ResponseEntity.ok(new RespuestaMensaje("Atributo eliminado exitosamente del servicio"));
    }

    /**
     * Sube la imagen de miniatura de un servicio al almacenamiento de documentos.
     *
     * @param archivo archivo de imagen a subir
     * @return la URL de la miniatura subida, con estado 201
     */
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
     *
     * @param request petición HTTP, de la cual se extrae la referencia de la miniatura solicitada
     * @return el contenido binario de la miniatura con su tipo de contenido y cabecera de caché
     * @throws ExcepcionRecursoNoEncontrado si la referencia no corresponde a una miniatura bajo el prefijo "servicios/"
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
