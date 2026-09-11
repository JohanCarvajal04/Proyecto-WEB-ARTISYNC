package uteq.edu.ec.artisync.controller.perfil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.VerificationDecisionRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.VerificationQueueResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.IdentityStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.VerificationResponse;
import uteq.edu.ec.artisync.entity.perfil.VerificationDocumentType;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;

import java.util.List;

/**
 * REQ-F-006/007. La IA solo asiste (ver /analisis-ia); únicamente /decision
 * escribe el estado final, y exige CERTIFICADO_REVISAR.
 */
@Tag(name = "Verificación", description = "Verificación de identidad y certificados, asistida por IA")
@RestController
@RequestMapping("/api/v1/verificaciones")
@RequiredArgsConstructor
public class VerificationController {

    private final IVerificationService verificacionServicio;

    /**
     * Solicita una verificación de identidad o de certificado, subiendo el documento correspondiente.
     *
     * @param tipo tipo de documento de verificación a subir
     * @param documento archivo del documento a verificar
     * @param userDetails usuario autenticado que solicita la verificación
     * @return la verificación creada, con estado 201
     * @throws ResourceNotFoundException si el usuario solicitante no existe
     * @throws BusinessRuleException si el usuario ya tiene una solicitud de este tipo en curso, o el documento no puede leerse
     */
    @Operation(summary = "Solicitar una verificación de identidad o certificado")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VerificationResponse> subir(
            @RequestParam("tipo") VerificationDocumentType tipo,
            @RequestParam("documento") MultipartFile documento,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        VerificationResponse respuesta = verificacionServicio.subir(userDetails.getIdUsuario(), tipo, documento);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene el estado de identidad del usuario autenticado, que gatea la publicación de servicios y la creación de pedidos.
     *
     * @param userDetails usuario autenticado
     * @return el estado de identidad del usuario
     */
    @Operation(summary = "Estado de identidad del usuario autenticado (gatea publicar servicios y crear pedidos)")
    @GetMapping("/mi-estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IdentityStatusResponse> obtenerMiEstadoIdentidad(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(verificacionServicio.obtenerEstadoIdentidad(userDetails.getIdUsuario()));
    }

    /**
     * Lista la cola de verificaciones pendientes de revisión.
     *
     * @param estado estado por el cual filtrar la cola (opcional)
     * @param limite cantidad máxima de resultados a devolver
     * @param offset cantidad de resultados a saltar, para paginación
     * @return listado de verificaciones en cola
     */
    @Operation(summary = "Cola de verificaciones pendientes de revisión")
    @GetMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<VerificationQueueResponse>> listarCola(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "20") int limite,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(verificacionServicio.listarCola(estado, limite, offset));
    }

    /**
     * Obtiene el detalle de una verificación.
     *
     * @param id identificador de la verificación
     * @param userDetails usuario autenticado que consulta la verificación
     * @return el detalle de la verificación
     * @throws ResourceNotFoundException si la verificación no existe
     */
    @Operation(summary = "Detalle de una verificación")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VerificationResponse> obtenerPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean esRevisor = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CERTIFICADO_REVISAR") || a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(verificacionServicio.obtenerPorId(id, userDetails.getIdUsuario(), esRevisor));
    }

    /**
     * Descarga el documento original de una verificación, para su revisión.
     *
     * @param id identificador de la verificación
     * @return el contenido binario del documento en formato JPEG
     * @throws ResourceNotFoundException si la verificación no existe
     */
    @Operation(summary = "Descargar el documento original para revisión")
    @GetMapping("/{id}/documento")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> obtenerDocumento(@PathVariable Long id) {
        byte[] documento = verificacionServicio.obtenerDocumento(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(documento);
    }

    /**
     * Pide a la IA un dictamen orientativo sobre una verificación; el dictamen no decide el estado final.
     *
     * @param id identificador de la verificación
     * @return la verificación con el dictamen de la IA
     * @throws ResourceNotFoundException si la verificación no existe
     * @throws BusinessRuleException si el documento de la verificación ya fue eliminado
     * @throws uteq.edu.ec.artisync.exception.AiServiceUnavailableException si el servicio de IA no está disponible
     */
    @Operation(summary = "Pedir a la IA un dictamen orientativo (no decide)")
    @PostMapping("/{id}/analisis-ia")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<VerificationResponse> analizarConIa(@PathVariable Long id) {
        return ResponseEntity.ok(verificacionServicio.analizarConIa(id));
    }

    /**
     * Registra la decisión del moderador sobre una verificación; es el único punto que cambia su estado final.
     *
     * @param id identificador de la verificación
     * @param peticion decisión del moderador, con el nuevo estado y una nota opcional
     * @param userDetails moderador autenticado que registra la decisión
     * @return la verificación con su estado actualizado
     * @throws ResourceNotFoundException si la verificación o el estado de verificación indicado no existen
     */
    @Operation(summary = "Registrar la decisión del moderador (único punto que cambia el estado)")
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<VerificationResponse> registrarDecision(
            @PathVariable Long id,
            @Valid @RequestBody VerificationDecisionRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        VerificationResponse respuesta = verificacionServicio.registrarDecision(
                id, userDetails.getIdUsuario(), peticion.idEstadoVerificacion(), peticion.notaModerador());
        return ResponseEntity.ok(respuesta);
    }
}
