package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorConfirmRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.seguridad.PrivacyService;
import uteq.edu.ec.artisync.service.seguridad.UserService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FileExtensions;
import uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

/** Gestión del propio perfil del usuario autenticado (datos, contraseña, privacidad). */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Gestión de Usuarios", description = "Endpoints protegidos para administración del perfil del usuario actual")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final PrivacyService privacidadService;
    private final DocumentStorage almacenamientoDocumentos;

    /**
     * Obtiene el perfil completo del usuario autenticado actual.
     *
     * @param principal usuario autenticado
     * @return el perfil del usuario autenticado
     */
    @Operation(summary = "Obtener el perfil completo del usuario autenticado actual")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getName()));
    }

    /**
     * Actualiza la información personal del usuario autenticado actual.
     *
     * @param principal usuario autenticado
     * @param request datos actualizados del usuario
     * @return el usuario actualizado
     */
    @Operation(summary = "Actualizar información personal del usuario autenticado actual")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(Principal principal, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(principal.getName(), request));
    }

    /**
     * Cambia la contraseña del usuario autenticado actual.
     *
     * @param principal usuario autenticado
     * @param request contraseña actual y nueva contraseña
     * @return mensaje de confirmación del cambio de contraseña
     */
    @Operation(summary = "Cambiar la contraseña del usuario autenticado actual")
    @PutMapping("/me/password")
    public ResponseEntity<RespuestaMensaje> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(principal.getName(), request));
    }

    /**
     * Desactiva la cuenta del usuario autenticado actual.
     *
     * @param principal usuario autenticado
     * @return mensaje de confirmación de la desactivación
     */
    @Operation(summary = "Desactivar la cuenta del usuario autenticado actual")
    @DeleteMapping("/me")
    public ResponseEntity<RespuestaMensaje> deleteOwnAccount(Principal principal) {
        return ResponseEntity.ok(userService.deleteOwnAccount(principal.getName()));
    }

    /**
     * REQ-NF-018: solicita la supresión real de los datos personales del
     * usuario autenticado (anonimización), distinta de la desactivación de
     * cuenta de {@link #deleteOwnAccount}. Idempotente: si ya se ejecutó
     * antes, lo informa sin repetir la operación.
     *
     * <p>Si el usuario tiene 2FA activo, exige un código TOTP o de respaldo
     * en el cuerpo de la petición ({@code request.codigo}) como verificación
     * adicional antes de una acción irreversible — el cuerpo es opcional
     * (sin {@code @Valid}: su campo es obligatorio solo condicionalmente,
     * esa validación la hace el servicio) para no romper a quien no tiene
     * 2FA activo.
     *
     * @param userDetails usuario autenticado
     * @param request     código 2FA opcional; requerido solo si el usuario tiene 2FA activo
     * @return mensaje de confirmación, incluyendo excepciones legales si las hay
     */
    @Operation(summary = "Solicitar la supresión (anonimización) de los datos personales del usuario actual")
    @PostMapping("/me/solicitud-supresion")
    public ResponseEntity<RespuestaMensaje> solicitarSupresionDatos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) TwoFactorConfirmRequest request) {
        String codigo = request != null ? request.getCodigo() : null;
        return ResponseEntity.ok(privacidadService.solicitarSupresionPropia(userDetails.getIdUsuario(), codigo));
    }

    /**
     * Cierra todas las sesiones activas del usuario autenticado actual.
     *
     * @param principal usuario autenticado
     * @return mensaje de confirmación del cierre de sesiones
     */
    @Operation(summary = "Cerrar todas las sesiones activas del usuario actual")
    @DeleteMapping("/me/sesiones")
    public ResponseEntity<RespuestaMensaje> revokeAllMySessions(Principal principal) {
        return ResponseEntity.ok(userService.revokeAllMySessions(principal.getName()));
    }

    /**
     * Sube o actualiza la foto de perfil del usuario autenticado actual.
     *
     * @param principal usuario autenticado
     * @param foto archivo de imagen a subir
     * @return el usuario con su foto de perfil actualizada
     */
    @Operation(summary = "Subir o actualizar la foto de perfil del usuario actual")
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfilePicture(Principal principal, @RequestParam("foto") org.springframework.web.multipart.MultipartFile foto) {
        return ResponseEntity.ok(userService.uploadProfilePicture(principal.getName(), foto));
    }

    /**
     * Sirve la foto de perfil públicamente (sin autenticación). La referencia
     * puede contener subdirectorios (e.g., "perfiles/uuid.jpg"), por eso se
     * captura con ** y se extrae manualmente del path.
     *
     * <p>Solo referencias bajo el prefijo "perfiles/" son válidas aquí: sin este
     * filtro, cualquiera podría pedir "verificacion/..." o "entregables/..." y
     * leer documentos privados (cédulas, títulos, entregables) sin autenticarse,
     * saltándose los @PreAuthorize de sus propios controladores.
     *
     * @param request petición HTTP, de la cual se extrae la referencia de la foto solicitada
     * @return el contenido binario de la foto con su tipo de contenido y cabecera de caché
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la referencia no corresponde a una foto bajo el prefijo "perfiles/"
     */
    @Operation(summary = "Servir la foto de perfil de un usuario (público)")
    @GetMapping("/foto/**")
    public ResponseEntity<byte[]> servirFotoPerfil(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/v1/usuarios/foto/";
        String referencia = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
        if (!referencia.startsWith(StoragePrefix.PERFILES + "/")) {
            throw new ResourceNotFoundException("Documento no disponible: " + referencia);
        }
        byte[] contenido = almacenamientoDocumentos.leer(referencia);
        String contentType = FileExtensions.contentTypeDe(referencia);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + TimeUnit.DAYS.toSeconds(7))
                .body(contenido);
    }
}

