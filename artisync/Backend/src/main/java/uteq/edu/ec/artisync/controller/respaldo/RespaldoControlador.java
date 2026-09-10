package uteq.edu.ec.artisync.controller.respaldo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionActualizarProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCambiarEstadoProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaProgramacion;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaRespaldo;
import uteq.edu.ec.artisync.service.respaldo.ArchivoRespaldo;
import uteq.edu.ec.artisync.service.respaldo.IRespaldoProgramacionServicio;
import uteq.edu.ec.artisync.service.respaldo.IRespaldoServicio;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

/**
 * Respaldos de base de datos (REQ-NF-024): disparo manual/programado,
 * FULL/INCREMENTAL, listado, descarga y eliminación, gestionados localmente.
 * Sin restauración: es deliberadamente un procedimiento manual documentado en
 * docs/despliegue/BACKUP.md, no una acción de la UI.
 */
@Tag(name = "Respaldos", description = "Respaldos de base de datos: creación, programación y gestión (REQ-NF-024)")
@RestController
@RequestMapping("/api/v1/admin/respaldos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RespaldoControlador {

    private final IRespaldoServicio respaldoServicio;
    private final IRespaldoProgramacionServicio programacionServicio;

    @Operation(summary = "Dispara un respaldo FULL o INCREMENTAL bajo demanda")
    @PostMapping
    @PreAuthorize("hasAuthority('RESPALDO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaRespaldo> crear(
            @Valid @RequestBody PeticionCrearRespaldo peticion, Authentication authentication) {
        RespuestaRespaldo respuesta = respaldoServicio.solicitarRespaldo(peticion.getTipoRespaldo(), authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }

    @Operation(summary = "Listado paginado y filtrado de respaldos")
    @GetMapping
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<RespuestaRespaldo>> listar(FiltroRespaldo filtro, Pageable pageable) {
        return ResponseEntity.ok(respaldoServicio.listar(filtro, pageable));
    }

    @Operation(summary = "Detalle de un respaldo")
    @GetMapping("/{idRespaldo}")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaRespaldo> obtenerPorId(@PathVariable Long idRespaldo) {
        return ResponseEntity.ok(respaldoServicio.obtenerPorId(idRespaldo));
    }

    @Operation(summary = "Descarga el archivo generado de un respaldo")
    @GetMapping("/{idRespaldo}/descargar")
    @PreAuthorize("hasAuthority('RESPALDO_DESCARGAR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> descargar(@PathVariable Long idRespaldo) {
        ArchivoRespaldo archivo = respaldoServicio.descargar(idRespaldo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .contentLength(archivo.tamanoBytes())
                .body(archivo.recurso());
    }

    @Operation(summary = "Elimina un respaldo guardado (rechaza si aún tiene incrementales que dependen de él)")
    @DeleteMapping("/{idRespaldo}")
    @PreAuthorize("hasAuthority('RESPALDO_ELIMINAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long idRespaldo) {
        respaldoServicio.eliminar(idRespaldo);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crea una programación recurrente de respaldos")
    @PostMapping("/programaciones")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaProgramacion> crearProgramacion(
            @Valid @RequestBody PeticionCrearProgramacion peticion, Authentication authentication) {
        RespuestaProgramacion respuesta = programacionServicio.crear(peticion, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Lista las programaciones de respaldo existentes")
    @GetMapping("/programaciones")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaProgramacion>> listarProgramaciones() {
        return ResponseEntity.ok(programacionServicio.listar());
    }

    @Operation(summary = "Detalle de una programación de respaldo")
    @GetMapping("/programaciones/{idProgramacion}")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaProgramacion> obtenerProgramacion(@PathVariable Long idProgramacion) {
        return ResponseEntity.ok(programacionServicio.obtenerPorId(idProgramacion));
    }

    @Operation(summary = "Actualiza una programación de respaldo existente")
    @PutMapping("/programaciones/{idProgramacion}")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaProgramacion> actualizarProgramacion(
            @PathVariable Long idProgramacion, @Valid @RequestBody PeticionActualizarProgramacion peticion) {
        return ResponseEntity.ok(programacionServicio.actualizar(idProgramacion, peticion));
    }

    @Operation(summary = "Activa o desactiva una programación de respaldo")
    @PatchMapping("/programaciones/{idProgramacion}/estado")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaProgramacion> cambiarEstadoProgramacion(
            @PathVariable Long idProgramacion, @Valid @RequestBody PeticionCambiarEstadoProgramacion peticion) {
        return ResponseEntity.ok(programacionServicio.cambiarEstado(idProgramacion, peticion.getActivo()));
    }

    @Operation(summary = "Elimina una programación de respaldo")
    @DeleteMapping("/programaciones/{idProgramacion}")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProgramacion(@PathVariable Long idProgramacion) {
        programacionServicio.eliminar(idProgramacion);
        return ResponseEntity.noContent().build();
    }
}
