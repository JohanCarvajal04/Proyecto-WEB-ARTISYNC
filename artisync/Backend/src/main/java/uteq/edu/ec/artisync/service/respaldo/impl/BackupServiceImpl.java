package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.BackupResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupStatus;
import uteq.edu.ec.artisync.entity.respaldo.Backup;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.respaldo.BackupRepository;
import uteq.edu.ec.artisync.scheduler.BackupExecutorService;
import uteq.edu.ec.artisync.scheduler.BackupRetentionScheduler;
import uteq.edu.ec.artisync.service.respaldo.BackupFile;
import uteq.edu.ec.artisync.service.respaldo.IBackupService;
import uteq.edu.ec.artisync.specification.respaldo.BackupSpecification;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements IBackupService {

    private final BackupRepository respaldoRepository;
    private final BackupExecutorService respaldoEjecutorServicio;
    private final BackupRetentionScheduler retencionScheduler;

    /**
     * Sin @Transactional a propósito: el INSERT que hace
     * BackupExecutorService.iniciarManual debe quedar comprometido antes
     * de que el hilo @Async (con su propia conexión) intente leer esa misma
     * fila -- si este método llevara @Transactional, el hilo async podría
     * arrancar antes de que el commit sea visible.
     */
    @Auditable(accion = "RESPALDO_CREAR", modulo = AuditModule.SISTEMA,
            entidad = "respaldos", idEntidad = "#resultado.idRespaldo",
            detalle = "{tipoRespaldo: #tipo}")
    @Override
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param tipo parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public BackupResponse solicitarRespaldo(BackupType tipo, String correoSolicitante) {
        if (respaldoRepository.existsByEstadoRespaldo(BackupStatus.EN_PROGRESO)) {
            throw new BusinessRuleException("Ya hay un respaldo en progreso. Espere a que termine antes de iniciar otro.");
        }
        Backup respaldo = respaldoEjecutorServicio.iniciarManual(tipo, correoSolicitante);
        return aRespuesta(respaldo);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PagedResponse<BackupResponse> listar(BackupFilter filtro, Pageable pageable) {
        Page<Backup> pagina = respaldoRepository.findAll(BackupSpecification.conFiltro(filtro), pageable);
        return PagedResponseBuilder.buildAndMap(pagina, this::aRespuesta);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idRespaldo identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public BackupResponse obtenerPorId(Long idRespaldo) {
        return aRespuesta(obtenerOFallar(idRespaldo));
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param idRespaldo identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public BackupFile descargar(Long idRespaldo) {
        Backup respaldo = obtenerOFallar(idRespaldo);
        if (respaldo.getRutaArchivo() == null) {
            throw new ResourceNotFoundException("El respaldo " + idRespaldo + " todavía no tiene un archivo generado");
        }
        Path ruta = Path.of(respaldo.getRutaArchivo());
        if (!Files.exists(ruta)) {
            throw new ResourceNotFoundException("El archivo del respaldo " + idRespaldo + " no existe en disco");
        }
        // FileSystemResource -> Spring transmite el InputStream a la respuesta
        // en streaming, nunca un byte[] completo en memoria (a diferencia de
        // GeneratedDocument/DocumentResponse, pensados para reportes
        // pequeños; un dump de BD puede ser mucho más grande).
        return new BackupFile(new FileSystemResource(ruta), respaldo.getNombreArchivo(), tamano(ruta));
    }

    @Auditable(accion = "RESPALDO_ELIMINAR", modulo = AuditModule.SISTEMA,
            entidad = "respaldos", idEntidad = "#idRespaldo")
    @Override
    @Transactional
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idRespaldo identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminar(Long idRespaldo) {
        Backup respaldo = obtenerOFallar(idRespaldo);
        if (respaldo.getEstadoRespaldo() == BackupStatus.EN_PROGRESO) {
            throw new BusinessRuleException("No se puede eliminar un respaldo en progreso.");
        }
        if (!retencionScheduler.esSeguroEliminar(respaldo)) {
            throw new BusinessRuleException(
                    "No se puede eliminar: existen respaldos incrementales que dependen de este FULL. Elimínelos primero.");
        }
        if (respaldo.getRutaArchivo() != null) {
            try {
                Files.deleteIfExists(Path.of(respaldo.getRutaArchivo()));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo borrar el archivo del respaldo: " + e.getMessage(), e);
            }
        }
        respaldoRepository.delete(respaldo);
    }

    private Backup obtenerOFallar(Long idRespaldo) {
        return respaldoRepository.findById(idRespaldo)
                .orElseThrow(() -> new ResourceNotFoundException("Backup no encontrado con ID: " + idRespaldo));
    }

    private long tamano(Path ruta) {
        try {
            return Files.size(ruta);
        } catch (IOException e) {
            return 0L;
        }
    }

    private BackupResponse aRespuesta(Backup respaldo) {
        return BackupResponse.builder()
                .idRespaldo(respaldo.getIdRespaldo())
                .tipoRespaldo(respaldo.getTipoRespaldo())
                .estadoRespaldo(respaldo.getEstadoRespaldo())
                .origen(respaldo.getOrigen())
                .idProgramacion(respaldo.getProgramacion() != null ? respaldo.getProgramacion().getIdProgramacion() : null)
                .idRespaldoFullBase(respaldo.getIdRespaldoFullBase())
                .nombreArchivo(respaldo.getNombreArchivo())
                .tamanoBytes(respaldo.getTamanoBytes())
                .fechaInicio(respaldo.getFechaInicio())
                .fechaFin(respaldo.getFechaFin())
                .duracionMs(respaldo.getDuracionMs())
                .mensajeError(respaldo.getMensajeError())
                .correoSolicitante(respaldo.getCorreoSolicitante())
                .build();
    }
}
