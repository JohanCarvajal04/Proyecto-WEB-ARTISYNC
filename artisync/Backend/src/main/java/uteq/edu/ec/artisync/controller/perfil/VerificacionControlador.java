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
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionDecisionVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaColaVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;

import java.util.List;

/**
 * REQ-F-006/007. La IA solo asiste (ver /analisis-ia); únicamente /decision
 * escribe el estado final, y exige CERTIFICADO_REVISAR.
 */
@Tag(name = "Verificación", description = "Verificación de identidad y certificados, asistida por IA")
@RestController
@RequestMapping("/api/v1/verificaciones")
@RequiredArgsConstructor
public class VerificacionControlador {

    private final IVerificacionServicio verificacionServicio;

    @Operation(summary = "Solicitar una verificación de identidad o certificado")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaVerificacion> subir(
            @RequestParam("tipo") TipoDocumentoVerificacion tipo,
            @RequestParam("documento") MultipartFile documento,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RespuestaVerificacion respuesta = verificacionServicio.subir(userDetails.getIdUsuario(), tipo, documento);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Cola de verificaciones pendientes de revisión")
    @GetMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaColaVerificacion>> listarCola(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "20") int limite,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(verificacionServicio.listarCola(estado, limite, offset));
    }

    @Operation(summary = "Detalle de una verificación")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaVerificacion> obtenerPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean esRevisor = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CERTIFICADO_REVISAR") || a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(verificacionServicio.obtenerPorId(id, userDetails.getIdUsuario(), esRevisor));
    }

    @Operation(summary = "Descargar el documento original para revisión")
    @GetMapping("/{id}/documento")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> obtenerDocumento(@PathVariable Long id) {
        byte[] documento = verificacionServicio.obtenerDocumento(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(documento);
    }

    @Operation(summary = "Pedir a la IA un dictamen orientativo (no decide)")
    @PostMapping("/{id}/analisis-ia")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaVerificacion> analizarConIa(@PathVariable Long id) {
        return ResponseEntity.ok(verificacionServicio.analizarConIa(id));
    }

    @Operation(summary = "Registrar la decisión del moderador (único punto que cambia el estado)")
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaVerificacion> registrarDecision(
            @PathVariable Long id,
            @Valid @RequestBody PeticionDecisionVerificacion peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RespuestaVerificacion respuesta = verificacionServicio.registrarDecision(
                id, userDetails.getIdUsuario(), peticion.idEstadoVerificacion(), peticion.notaModerador());
        return ResponseEntity.ok(respuesta);
    }
}
