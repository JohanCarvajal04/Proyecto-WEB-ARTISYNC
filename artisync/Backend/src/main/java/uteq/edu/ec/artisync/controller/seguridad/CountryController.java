package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.CountryRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.CountryResponse;
import uteq.edu.ec.artisync.service.seguridad.CountryService;

import java.util.List;

/** Consulta y gestión del catálogo maestro de países. */
@RestController
@RequestMapping("/api/v1/paises")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Países", description = "Endpoints para consulta pública y administración (CUD) de países")
public class CountryController {

    private final CountryService paisService;

    /**
     * Lista todos los países del catálogo, ordenados alfabéticamente.
     *
     * @return listado de países
     */
    @Operation(summary = "Listar todos los países ordenados alfabéticamente")
    @GetMapping
    public ResponseEntity<List<CountryResponse>> getAllCountries() {
        return ResponseEntity.ok(paisService.getAllCountries());
    }

    /**
     * Lista solo los países activos del catálogo, ordenados alfabéticamente.
     *
     * @return listado de países activos
     */
    @Operation(summary = "Listar solo los países activos ordenados alfabéticamente")
    @GetMapping("/activos")
    public ResponseEntity<List<CountryResponse>> getActiveCountries() {
        return ResponseEntity.ok(paisService.getActiveCountries());
    }

    /**
     * Obtiene un país por su identificador.
     *
     * @param id identificador del país
     * @return el país solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el país no existe
     */
    @Operation(summary = "Obtener un país por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<CountryResponse> getCountryById(@PathVariable Long id) {
        return ResponseEntity.ok(paisService.getCountryById(id));
    }

    /**
     * Crea un nuevo país en el catálogo.
     *
     * @param request datos del país a crear
     * @return el país creado, con estado 201
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si ya existe un país con el mismo nombre
     */
    @Operation(summary = "Crear un nuevo país", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('PAIS_CREAR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paisService.createCountry(request));
    }

    /**
     * Actualiza el nombre de un país existente.
     *
     * @param id identificador del país a actualizar
     * @param request datos actualizados del país
     * @return el país actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el país no existe
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si ya existe otro país con el mismo nombre
     */
    @Operation(summary = "Actualizar el nombre de un país existente", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('PAIS_EDITAR') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CountryResponse> updateCountry(@PathVariable Long id, @Valid @RequestBody CountryRequest request) {
        return ResponseEntity.ok(paisService.updateCountry(id, request));
    }

    /**
     * Activa o desactiva un país del catálogo.
     *
     * @param id identificador del país
     * @return mensaje de confirmación con el nuevo estado del país
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el país no existe
     */
    @Operation(summary = "Eliminar un país si no tiene usuarios asociados", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('PAIS_ELIMINAR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<RespuestaMensaje> deleteCountry(@PathVariable Long id) {
        return ResponseEntity.ok(paisService.deleteCountry(id));
    }
}

