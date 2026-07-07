package uteq.edu.ec.artisync.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.request.PaisRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.PaisResponse;
import uteq.edu.ec.artisync.service.PaisService;

import java.util.List;

@RestController
@RequestMapping("/api/paises")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Países", description = "Endpoints para consulta pública y administración (CUD) de países")
public class PaisController {

    private final PaisService paisService;

    @Operation(summary = "Listar todos los países ordenados alfabéticamente")
    @GetMapping
    public ResponseEntity<List<PaisResponse>> getAllPaises() {
        return ResponseEntity.ok(paisService.getAllPaises());
    }

    @Operation(summary = "Obtener un país por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<PaisResponse> getPaisById(@PathVariable Long id) {
        return ResponseEntity.ok(paisService.getPaisById(id));
    }

    @Operation(summary = "Crear un nuevo país", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('PAIS_CREAR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PaisResponse> createPais(@Valid @RequestBody PaisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paisService.createPais(request));
    }

    @Operation(summary = "Actualizar el nombre de un país existente", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('PAIS_EDITAR') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PaisResponse> updatePais(@PathVariable Long id, @Valid @RequestBody PaisRequest request) {
        return ResponseEntity.ok(paisService.updatePais(id, request));
    }

    @Operation(summary = "Eliminar un país si no tiene usuarios asociados", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('PAIS_ELIMINAR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePais(@PathVariable Long id) {
        return ResponseEntity.ok(paisService.deletePais(id));
    }
}
