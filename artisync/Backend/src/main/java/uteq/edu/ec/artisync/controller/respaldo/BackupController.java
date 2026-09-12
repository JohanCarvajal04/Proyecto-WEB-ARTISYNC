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
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.dto.peticion.respaldo.UpdateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.ChangeScheduleStatusRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateBackupRequest;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.ScheduleResponse;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.BackupResponse;
import uteq.edu.ec.artisync.service.respaldo.BackupFile;
import uteq.edu.ec.artisync.service.respaldo.IBackupScheduleService;
import uteq.edu.ec.artisync.service.respaldo.IBackupService;
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
public class BackupController {

    private final IBackupService respaldoServicio;
    private final IBackupScheduleService programacionServicio;

    /**
     * Dispara la creación de un nuevo respaldo de base de datos bajo demanda.
     * @param peticion datos de la solicitud, incluyendo el tipo de respaldo (FULL o INCREMENTAL)
     * @param authentication usuario administrador que solicita el respaldo
     * @return el registro del respaldo solicitado con estado inicial de procesamiento
     */
    @Operation(summary = "Dispara un respaldo FULL o INCREMENTAL bajo demanda")
    @PostMapping
    @PreAuthorize("hasAuthority('RESPALDO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<BackupResponse> crear(
            @Valid @RequestBody CreateBackupRequest peticion, Authentication authentication) {
        BackupResponse respuesta = respaldoServicio.solicitarRespaldo(peticion.getTipoRespaldo(), authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }

    /**
     * Lista los respaldos de base de datos generados, permitiendo filtros y paginación.
     * @param filtro criterios opcionales para buscar respaldos específicos
     * @param pageable configuración de la paginación
     * @return listado paginado de los respaldos que coinciden con el filtro
     */
    @Operation(summary = "Listado paginado y filtrado de respaldos")
    @GetMapping
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<BackupResponse>> listar(BackupFilter filtro, Pageable pageable) {
        return ResponseEntity.ok(respaldoServicio.listar(filtro, pageable));
    }

    /**
     * Obtiene el detalle completo de un respaldo específico registrado en el sistema.
     * @param idRespaldo identificador único del respaldo
     * @return detalle del respaldo solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el respaldo indicado no existe
     */
    @Operation(summary = "Detalle de un respaldo")
    @GetMapping("/{idRespaldo}")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<BackupResponse> obtenerPorId(@PathVariable Long idRespaldo) {
        return ResponseEntity.ok(respaldoServicio.obtenerPorId(idRespaldo));
    }

    /**
     * Descarga físicamente el archivo del respaldo generado desde el sistema de almacenamiento.
     * @param idRespaldo identificador único del respaldo a descargar
     * @return el archivo comprimido del respaldo como un stream de octetos
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el archivo físico no existe en disco
     */
    @Operation(summary = "Descarga el archivo generado de un respaldo")
    @GetMapping("/{idRespaldo}/descargar")
    @PreAuthorize("hasAuthority('RESPALDO_DESCARGAR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> descargar(@PathVariable Long idRespaldo) {
        BackupFile archivo = respaldoServicio.descargar(idRespaldo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .contentLength(archivo.tamanoBytes())
                .body(archivo.recurso());
    }

    /**
     * Elimina el registro y el archivo físico de un respaldo del sistema.
     * @param idRespaldo identificador del respaldo a eliminar
     * @return respuesta sin contenido confirmando la eliminación exitosa
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si es un respaldo FULL y existen respaldos INCREMENTALES que dependen de él
     */
    @Operation(summary = "Elimina un respaldo guardado (rechaza si aún tiene incrementales que dependen de él)")
    @DeleteMapping("/{idRespaldo}")
    @PreAuthorize("hasAuthority('RESPALDO_ELIMINAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long idRespaldo) {
        respaldoServicio.eliminar(idRespaldo);
        return ResponseEntity.noContent().build();
    }

    /**
     * Configura una nueva tarea programada (CRON) para la ejecución automática de respaldos.
     * @param peticion configuración de la frecuencia (CRON) y el tipo de respaldo a programar
     * @param authentication usuario administrador que crea la programación
     * @return el detalle de la programación recién creada
     */
    @Operation(summary = "Crea una programación recurrente de respaldos")
    @PostMapping("/programaciones")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> crearProgramacion(
            @Valid @RequestBody CreateScheduleRequest peticion, Authentication authentication) {
        ScheduleResponse respuesta = programacionServicio.crear(peticion, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene el listado de todas las tareas automáticas de respaldo configuradas.
     * @return lista completa de programaciones existentes
     */
    @Operation(summary = "Lista las programaciones de respaldo existentes")
    @GetMapping("/programaciones")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleResponse>> listarProgramaciones() {
        return ResponseEntity.ok(programacionServicio.listar());
    }

    /**
     * Obtiene los detalles de una programación de respaldo específica.
     * @param idProgramacion identificador de la programación a consultar
     * @return detalles de la programación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación indicada no existe
     */
    @Operation(summary = "Detalle de una programación de respaldo")
    @GetMapping("/programaciones/{idProgramacion}")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> obtenerProgramacion(@PathVariable Long idProgramacion) {
        return ResponseEntity.ok(programacionServicio.obtenerPorId(idProgramacion));
    }

    /**
     * Modifica la configuración (ej. expresión CRON) de una tarea programada de respaldo.
     * @param idProgramacion identificador de la programación a modificar
     * @param peticion nuevos parámetros de configuración para la programación
     * @return la programación actualizada con los nuevos valores
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación no existe
     */
    @Operation(summary = "Actualiza una programación de respaldo existente")
    @PutMapping("/programaciones/{idProgramacion}")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> actualizarProgramacion(
            @PathVariable Long idProgramacion, @Valid @RequestBody UpdateScheduleRequest peticion) {
        return ResponseEntity.ok(programacionServicio.actualizar(idProgramacion, peticion));
    }

    /**
     * Habilita o deshabilita la ejecución automática de una programación sin eliminarla.
     * @param idProgramacion identificador de la programación a alternar
     * @param peticion estado deseado (activo/inactivo) para la programación
     * @return la programación actualizada con su nuevo estado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación no existe
     */
    @Operation(summary = "Activa o desactiva una programación de respaldo")
    @PatchMapping("/programaciones/{idProgramacion}/estado")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> cambiarEstadoProgramacion(
            @PathVariable Long idProgramacion, @Valid @RequestBody ChangeScheduleStatusRequest peticion) {
        return ResponseEntity.ok(programacionServicio.cambiarEstado(idProgramacion, peticion.getActivo()));
    }

    /**
     * Elimina definitivamente una tarea programada de respaldos automáticos.
     * @param idProgramacion identificador de la programación a borrar
     * @return respuesta sin contenido confirmando la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación indicada no existe
     */
    @Operation(summary = "Elimina una programación de respaldo")
    @DeleteMapping("/programaciones/{idProgramacion}")
    @PreAuthorize("hasAuthority('RESPALDO_PROGRAMAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProgramacion(@PathVariable Long idProgramacion) {
        programacionServicio.eliminar(idProgramacion);
        return ResponseEntity.noContent().build();
    }
}
