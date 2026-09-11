package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPortafolioServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portafolios")
@RequiredArgsConstructor
public class PortafolioControlador {

    private final IPortafolioServicio portafolioServicio;

    /**
     * Crea el portafolio de un perfil de creador.
     *
     * @param peticion datos del portafolio a crear
     * @param userDetails usuario autenticado que crea el portafolio
     * @return el portafolio creado, con estado 201
     * @throws DuplicateResourceException si el perfil de creador ya cuenta con un portafolio registrado
     * @throws ResourceNotFoundException si el perfil de creador no existe
     * @throws BusinessRuleException si el usuario no tiene permisos para crear un portafolio para ese perfil
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaPortafolio> crearPortafolio(
            @Valid @RequestBody PeticionCrearPortafolio peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RespuestaPortafolio respuesta = portafolioServicio.crearPortafolio(peticion, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene un portafolio por su identificador.
     *
     * @param id identificador del portafolio
     * @return el portafolio solicitado
     * @throws ResourceNotFoundException si el portafolio no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<RespuestaPortafolio> obtenerPortafolioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(portafolioServicio.obtenerPortafolioPorId(id));
    }

    /**
     * Obtiene el portafolio asociado a un perfil de creador.
     *
     * @param idPerfil identificador del perfil de creador
     * @return el portafolio del perfil
     * @throws ResourceNotFoundException si no existe portafolio para el perfil
     */
    @GetMapping("/perfil/{idPerfil}")
    public ResponseEntity<RespuestaPortafolio> obtenerPortafolioPorPerfil(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(portafolioServicio.obtenerPortafolioPorPerfil(idPerfil));
    }

    /**
     * Lista todos los portafolios del sistema.
     *
     * @return listado de portafolios
     */
    @GetMapping
    public ResponseEntity<List<RespuestaPortafolio> > listarPortafolios() {
        return ResponseEntity.ok(portafolioServicio.listarPortafolios());
    }

    /**
     * Actualiza los datos de un portafolio existente.
     *
     * @param id identificador del portafolio a actualizar
     * @param peticion datos actualizados del portafolio
     * @param userDetails usuario autenticado que solicita la actualización
     * @return el portafolio actualizado
     * @throws ResourceNotFoundException si el portafolio no existe
     * @throws BusinessRuleException si el usuario no tiene permisos para modificar el portafolio
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaPortafolio> actualizarPortafolio(
            @PathVariable Long id,
            @Valid @RequestBody PeticionActualizarPortafolio peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(portafolioServicio.actualizarPortafolio(id, peticion, userDetails.getIdUsuario()));
    }

    /**
     * Registra una visita al portafolio, incrementando su contador de visitas.
     *
     * @param id identificador del portafolio visitado
     * @param userDetails usuario autenticado que registra la visita
     * @return mensaje de confirmación del registro de la visita
     * @throws ResourceNotFoundException si el portafolio no existe
     */
    @PostMapping("/{id}/visita")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> registrarVisita(
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
     * @throws ResourceNotFoundException si el portafolio no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAFOLIO_MODERAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarPortafolio(@PathVariable Long id) {
        portafolioServicio.eliminarPortafolio(id);
        return ResponseEntity.ok(new RespuestaMensaje("Portafolio eliminado exitosamente"));
    }
}
