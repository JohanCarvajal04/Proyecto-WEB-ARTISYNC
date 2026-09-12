package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreatePortfolioRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdatePortfolioRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PortfolioResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPortfolioService;

import java.util.List;

/** Gestión del portafolio de un creador. */
@RestController
@RequestMapping("/api/v1/portafolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final IPortfolioService portafolioServicio;

    /**
     * Crea el portafolio de un perfil de creador.
     *
     * @param peticion datos del portafolio a crear
     * @param userDetails usuario autenticado que crea el portafolio
     * @return el portafolio creado, con estado 201
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el perfil de creador ya cuenta con un portafolio registrado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil de creador no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario no tiene permisos para crear un portafolio para ese perfil
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PortfolioResponse respuesta = portafolioServicio.createPortfolio(peticion, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene un portafolio por su identificador.
     *
     * @param id identificador del portafolio
     * @return el portafolio solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable Long id) {
        return ResponseEntity.ok(portafolioServicio.getPortfolioById(id));
    }

    /**
     * Obtiene el portafolio asociado a un perfil de creador.
     *
     * @param idPerfil identificador del perfil de creador
     * @return el portafolio del perfil
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si no existe portafolio para el perfil
     */
    @GetMapping("/perfil/{idPerfil}")
    public ResponseEntity<PortfolioResponse> getPortfolioByProfile(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(portafolioServicio.getPortfolioByProfile(idPerfil));
    }

    /**
     * Lista todos los portafolios del sistema.
     *
     * @return listado de portafolios
     */
    @GetMapping
    public ResponseEntity<List<PortfolioResponse> > listPortfolios() {
        return ResponseEntity.ok(portafolioServicio.listPortfolios());
    }

    /**
     * Actualiza los datos de un portafolio existente.
     *
     * @param id identificador del portafolio a actualizar
     * @param peticion datos actualizados del portafolio
     * @param userDetails usuario autenticado que solicita la actualización
     * @return el portafolio actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario no tiene permisos para modificar el portafolio
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePortfolioRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(portafolioServicio.updatePortfolio(id, peticion, userDetails.getIdUsuario()));
    }

    /**
     * Registra una visita al portafolio, incrementando su contador de visitas.
     *
     * @param id identificador del portafolio visitado
     * @param userDetails usuario autenticado que registra la visita
     * @return mensaje de confirmación del registro de la visita
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    @PostMapping("/{id}/visita")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> recordVisit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        portafolioServicio.incrementarVisitas(id, userDetails.getIdUsuario());
        return ResponseEntity.ok(new RespuestaMensaje("Visita al portafolio incrementada"));
    }

    /**
     * Elimina un portafolio como acción de moderación.
     *
     * @param id identificador del portafolio a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> deletePortfolio(@PathVariable Long id) {
        portafolioServicio.deletePortfolio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Portfolio eliminado exitosamente"));
    }
}
