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
import uteq.edu.ec.artisync.dto.peticion.perfil.CreatePortfolioItemRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PortfolioItemResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPortfolioItemService;

import java.util.List;

/**
 * Obras del portafolio. Las rutas de lectura cuelgan de /api/v1/portafolios,
 * que SecurityConfig deja abierto en GET: un portafolio público debe poder
 * verse sin sesión, y el servicio es quien filtra los privados.
/**
 * Controlador REST (PortfolioItemController)
 * 
 * Gestiona el ciclo de vida de los items u obras dentro de un portafolio publico.
 * Expone endpoints para subir archivos multimedia, listarlos, descargarlos y eliminarlos.
 * Incorpora protecciones contra XSS al forzar la descarga de archivos (Content-Disposition: attachment).
 */
@RestController
@RequestMapping("/api/v1/portafolios")
@RequiredArgsConstructor
public class PortfolioItemController {

    private final IPortfolioItemService itemServicio;

    /**
     * Sube una nueva obra (ítem) a un portafolio.
     *
     * @param idPortafolio identificador del portafolio
     * @param userDetails usuario autenticado que sube la obra
     * @param datos datos de la obra a crear
     * @param archivo archivo multimedia de la obra
     * @return el ítem de portafolio creado, con estado 201
     * @throws ResourceNotFoundException si el portafolio no existe
     * @throws BusinessRuleException si el usuario no es el dueño del portafolio o se alcanzó el máximo de obras permitidas
     */
    @PostMapping(value = "/{idPortafolio}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<PortfolioItemResponse> subirItem(
            @PathVariable Long idPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestPart("datos") CreatePortfolioItemRequest datos,
            @RequestPart("archivo") MultipartFile archivo) {
        PortfolioItemResponse respuesta = itemServicio.subirItem(
                idPortafolio, userDetails.getIdUsuario(), datos, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Lista las obras de un portafolio, respetando su visibilidad.
     *
     * @param idPortafolio identificador del portafolio
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return listado de ítems del portafolio
     * @throws ResourceNotFoundException si el portafolio no existe
     * @throws BusinessRuleException si el portafolio no es público y el usuario no es su dueño
     */
    @GetMapping("/{idPortafolio}/items")
    public ResponseEntity<List<PortfolioItemResponse>> listarItems(
            @PathVariable Long idPortafolio,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(itemServicio.listarItems(idPortafolio, idDe(userDetails)));
    }

    /**
     * Obtiene una obra de portafolio por su identificador, respetando su visibilidad.
     *
     * @param idItem identificador de la obra
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return el ítem de portafolio solicitado
     * @throws ResourceNotFoundException si la obra no existe
     * @throws BusinessRuleException si el portafolio no es público y el usuario no es su dueño
     */
    @GetMapping("/items/{idItem}")
    public ResponseEntity<PortfolioItemResponse> obtenerItem(
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
     *
     * @param idItem identificador de la obra a descargar
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return el contenido binario del archivo, como adjunto
     * @throws ResourceNotFoundException si la obra no existe
     * @throws BusinessRuleException si el portafolio no es público y el usuario no es su dueño
     */
    @GetMapping("/items/{idItem}/archivo")
    public ResponseEntity<byte[]> descargarArchivo(
            @PathVariable Long idItem,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        IPortfolioItemService.ArchivoItem archivo =
                itemServicio.descargarArchivo(idItem, idDe(userDetails));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreSugerido()).toString())
                .body(archivo.contenido());
    }

    /**
     * Actualiza los datos de una obra de portafolio.
     *
     * @param idItem identificador de la obra a actualizar
     * @param userDetails usuario autenticado que solicita la actualización
     * @param datos datos actualizados de la obra
     * @return el ítem de portafolio actualizado
     * @throws ResourceNotFoundException si la obra no existe
     * @throws BusinessRuleException si el usuario no es el dueño del portafolio
     */
    @PutMapping("/items/{idItem}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<PortfolioItemResponse> actualizarItem(
            @PathVariable Long idItem,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePortfolioItemRequest datos) {
        return ResponseEntity.ok(itemServicio.actualizarItem(idItem, userDetails.getIdUsuario(), datos));
    }

    /**
     * Elimina una obra de un portafolio.
     *
     * @param idItem identificador de la obra a eliminar
     * @param userDetails usuario autenticado que solicita la eliminación
     * @return mensaje de confirmación de la eliminación
     * @throws ResourceNotFoundException si la obra no existe
     * @throws BusinessRuleException si el usuario no es el dueño del portafolio
     */
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
