package uteq.edu.ec.artisync.controller.social;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.social.UpdateRaffleRequest;
import uteq.edu.ec.artisync.dto.peticion.social.CreateRaffleRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.WinnerResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.ParticipantResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.RaffleResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.social.RaffleService;

import java.util.List;

/**
 * Controlador de sorteos configurables.
 * RF-23: CRUD, participación y consulta de ganadores.
 */
@Tag(name = "Sorteos", description = "Gestión de sorteos configurables por el creador")
@RestController
@RequiredArgsConstructor
public class RaffleController {

    private final RaffleService sorteoService;

    // =========================================================================
    // CRUD de Sorteos
    // =========================================================================

    /**
     * Crea un nuevo sorteo para el creador autenticado.
     *
     * @param peticion datos del sorteo a crear
     * @param userDetails usuario autenticado que crea el sorteo
     * @return el sorteo creado, con estado 201
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si los datos del sorteo violan alguna regla de negocio
     */
    @Operation(summary = "Crear un nuevo sorteo (CREADOR)")
    @PostMapping("/api/v1/sorteos")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RaffleResponse> createRaffle(
            @Valid @RequestBody CreateRaffleRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sorteoService.createRaffle(userDetails.getIdUsuario(), peticion));
    }

    /**
     * Obtiene el detalle público de un sorteo.
     *
     * @param idSorteo identificador del sorteo
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return el detalle del sorteo solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    @Operation(summary = "Obtener detalle de un sorteo (público)")
    @GetMapping("/api/v1/sorteos/{idSorteo}")
    public ResponseEntity<RaffleResponse> getRaffle(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(sorteoService.getRaffle(idSorteo, idUsuarioActual));
    }

    /**
     * Edita un sorteo existente, sujeto a las restricciones de edición del negocio.
     *
     * @param idSorteo identificador del sorteo a editar
     * @param peticion datos actualizados del sorteo
     * @param userDetails usuario autenticado que solicita la edición
     * @return el sorteo actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo no puede editarse en su estado actual
     */
    @Operation(summary = "Editar un sorteo (CREADOR, con restricciones)")
    @PutMapping("/api/v1/sorteos/{idSorteo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RaffleResponse> updateRaffle(
            @PathVariable Long idSorteo,
            @Valid @RequestBody UpdateRaffleRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                sorteoService.updateRaffle(idSorteo, userDetails.getIdUsuario(), peticion));
    }

    /**
     * Elimina un sorteo que aún no tiene participantes.
     *
     * @param idSorteo identificador del sorteo a eliminar
     * @param userDetails usuario autenticado que solicita la eliminación
     * @return mensaje de confirmación de la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo ya tiene participantes
     */
    @Operation(summary = "Eliminar un sorteo sin participantes (CREADOR)")
    @DeleteMapping("/api/v1/sorteos/{idSorteo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> deleteRaffle(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(sorteoService.deleteRaffle(idSorteo, userDetails.getIdUsuario()));
    }

    /**
     * Lista los sorteos de un creador.
     *
     * @param idPerfil identificador del perfil de creador
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return listado de sorteos del creador
     */
    @Operation(summary = "Listar sorteos de un creador (público)")
    @GetMapping("/api/v1/creadores/{idPerfil}/sorteos")
    public ResponseEntity<List<RaffleResponse>> listRafflesByCreator(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(sorteoService.listRafflesByCreator(idPerfil, idUsuarioActual));
    }

    /**
     * Lista los sorteos actualmente activos.
     *
     * @param userDetails usuario autenticado (opcional, puede ser {@code null} para acceso anónimo)
     * @return listado de sorteos activos
     */
    @Operation(summary = "Listar sorteos activos (público)")
    @GetMapping("/api/v1/sorteos/activos")
    public ResponseEntity<List<RaffleResponse>> listActiveRaffles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(sorteoService.listActiveRaffles(idUsuarioActual));
    }

    // =========================================================================
    // Participación
    // =========================================================================

    /**
     * Inscribe al usuario autenticado como participante en un sorteo.
     *
     * @param idSorteo identificador del sorteo
     * @param userDetails usuario autenticado que se inscribe
     * @return la participación creada, con estado 201
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo no está activo, aún no ha comenzado o el periodo de inscripción ha finalizado
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el usuario ya está inscrito en el sorteo
     */
    @Operation(summary = "Inscribirse en un sorteo")
    @PostMapping("/api/v1/sorteos/{idSorteo}/joinRaffle")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ParticipantResponse> joinRaffle(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sorteoService.joinRaffle(idSorteo, userDetails.getIdUsuario()));
    }

    /**
     * Cancela la inscripción del usuario autenticado en un sorteo.
     *
     * @param idSorteo identificador del sorteo
     * @param userDetails usuario autenticado que cancela su inscripción
     * @return mensaje de confirmación de la cancelación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo ya ha finalizado
     */
    @Operation(summary = "Cancelar inscripción en un sorteo")
    @DeleteMapping("/api/v1/sorteos/{idSorteo}/joinRaffle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> cancelParticipation(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                sorteoService.cancelParticipation(idSorteo, userDetails.getIdUsuario()));
    }

    /**
     * Lista los participantes inscritos en un sorteo.
     *
     * @param idSorteo identificador del sorteo
     * @return listado de participantes del sorteo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    @Operation(summary = "Listar participantes de un sorteo")
    @GetMapping("/api/v1/sorteos/{idSorteo}/participantes")
    public ResponseEntity<List<ParticipantResponse>> listParticipants(
            @PathVariable Long idSorteo) {
        return ResponseEntity.ok(sorteoService.listParticipants(idSorteo));
    }

    /**
     * Lista los ganadores de un sorteo, disponible solo después del cierre.
     *
     * @param idSorteo identificador del sorteo
     * @return listado de ganadores del sorteo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    @Operation(summary = "Ver ganadores del sorteo (solo post-cierre)")
    @GetMapping("/api/v1/sorteos/{idSorteo}/ganadores")
    public ResponseEntity<List<WinnerResponse>> listWinners(
            @PathVariable Long idSorteo) {
        return ResponseEntity.ok(sorteoService.listWinners(idSorteo));
    }
}
