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
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateAttributeRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateOfferingRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateAttributeRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateOfferingRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.AttributeResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaUrl;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.service.catalogo.IOfferingCatalogService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FileExtensions;
import uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Gestión de servicios del catálogo: creación, edición, atributos dinámicos y su miniatura. */
@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class OfferingController {

    private final IOfferingCatalogService servicioCatalogoServicio;
    private final DocumentStorage almacenamientoDocumentos;

    /**
     * Crea un nuevo servicio para el perfil de creador indicado.
     *
     * @param idPerfilCreador identificador del perfil de creador propietario del servicio
     * @param peticion datos del servicio a crear
     * @return el servicio creado, con estado 201
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el precio es menor a 0.01 USD
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil de creador no existe
     */
    @PostMapping("/creador/{idPerfilCreador}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<OfferingResponse> crearServicio(
            @PathVariable Long idPerfilCreador,
            @Valid @RequestBody CreateOfferingRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioCatalogoServicio.crearServicio(idPerfilCreador, peticion));
    }

    /**
     * Actualiza los datos de un servicio existente.
     *
     * @param id identificador del servicio a actualizar
     * @param peticion datos actualizados del servicio
     * @return el servicio actualizado
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el precio es menor a 0.01 USD o si el servicio queda sin subcategorías
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<OfferingResponse> actualizarServicio(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferingRequest peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarServicio(id, peticion));
    }

    /**
     * Obtiene el detalle de un servicio por su identificador.
     *
     * @param id identificador del servicio
     * @return el servicio solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<OfferingResponse> obtenerServicioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.obtenerServicioPorId(id));
    }

    /**
     * Elimina un servicio existente.
     *
     * @param id identificador del servicio a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarServicio(@PathVariable Long id) {
        servicioCatalogoServicio.eliminarServicio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Offering eliminado exitosamente"));
    }

    /**
     * Lista los servicios de un creador, opcionalmente filtrados por estado de publicación.
     *
     * @param idPerfilCreador identificador del perfil de creador
     * @param estadoPublicacion estado de publicación por el cual filtrar (opcional)
     * @return listado resumido de los servicios del creador
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil de creador no existe
     */
    @GetMapping("/creador/{idPerfilCreador}")
    public ResponseEntity<List<OfferingSummaryResponse>> listarServiciosPorCreador(
            @PathVariable Long idPerfilCreador,
            @RequestParam(required = false) String estadoPublicacion) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarServiciosPorCreador(idPerfilCreador, estadoPublicacion));
    }

    /**
     * Lista los atributos personalizados asociados a un servicio.
     *
     * @param id identificador del servicio
     * @return listado de atributos del servicio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    @GetMapping("/{id}/atributos")
    public ResponseEntity<List<AttributeResponse>> listarAtributosPorServicio(@PathVariable Long id) {
        return ResponseEntity.ok(servicioCatalogoServicio.listarAtributosPorServicio(id));
    }

    /**
     * Agrega un atributo personalizado a un servicio.
     *
     * @param id identificador del servicio
     * @param peticion datos del atributo a agregar
     * @return el atributo creado, con estado 201
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si se alcanzó el límite de atributos permitidos o el atributo ya está asociado al servicio
     */
    @PostMapping("/{id}/atributos")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<AttributeResponse> agregarAtributo(
            @PathVariable Long id,
            @Valid @RequestBody CreateAttributeRequest peticion) {
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
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio o el atributo no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el atributo no pertenece al servicio indicado
     */
    @PutMapping("/{id}/atributos/{idAtributo}")
    @PreAuthorize("hasAuthority('SERVICIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<AttributeResponse> actualizarAtributo(
            @PathVariable Long id,
            @PathVariable Long idAtributo,
            @Valid @RequestBody UpdateAttributeRequest peticion) {
        return ResponseEntity.ok(servicioCatalogoServicio.actualizarAtributo(id, idAtributo, peticion));
    }

    /**
     * Elimina un atributo personalizado de un servicio.
     *
     * @param id identificador del servicio
     * @param idAtributo identificador del atributo a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio o el atributo no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el atributo no pertenece al servicio indicado
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
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la referencia no corresponde a una miniatura bajo el prefijo "servicios/"
     */
    @GetMapping("/miniatura/**")
    public ResponseEntity<byte[]> servirMiniatura(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/v1/servicios/miniatura/";
        String referencia = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
        if (!referencia.startsWith(StoragePrefix.SERVICIOS + "/")) {
            throw new ResourceNotFoundException("Miniatura no disponible: " + referencia);
        }
        byte[] contenido = almacenamientoDocumentos.leer(referencia);
        String contentType = FileExtensions.contentTypeDe(referencia);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + TimeUnit.DAYS.toSeconds(7))
                .body(contenido);
    }
}
