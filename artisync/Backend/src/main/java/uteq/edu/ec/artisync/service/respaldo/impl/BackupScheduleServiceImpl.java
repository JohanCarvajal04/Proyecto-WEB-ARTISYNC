package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.respaldo.UpdateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateScheduleRequest;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.ScheduleResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupSchedule;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.respaldo.BackupScheduleRepository;
import uteq.edu.ec.artisync.service.respaldo.IBackupScheduleService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupScheduleServiceImpl implements IBackupScheduleService {

    private final BackupScheduleRepository programacionRepository;

    @Auditable(accion = "RESPALDO_PROGRAMACION_CREAR", modulo = AuditModule.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#resultado.idProgramacion",
            detalle = "{nombre: #peticion.nombre, tipoRespaldo: #peticion.tipoRespaldo}")
    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @param creadoPor parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ScheduleResponse crear(CreateScheduleRequest peticion, String creadoPor) {
        LocalDateTime proximaEjecucion = calcularProximaEjecucion(peticion.getExpresionCron());
        LocalDateTime ahora = LocalDateTime.now();

        BackupSchedule programacion = BackupSchedule.builder()
                .nombre(peticion.getNombre())
                .tipoRespaldo(peticion.getTipoRespaldo())
                .expresionCron(peticion.getExpresionCron())
                .retencionDias(peticion.getRetencionDias())
                .activo(true)
                .proximaEjecucion(proximaEjecucion)
                .creadoPor(creadoPor)
                .fechaCreacion(ahora)
                .actualizadoEn(ahora)
                .build();

        return aRespuesta(programacionRepository.save(programacion));
    }

    @Auditable(accion = "RESPALDO_PROGRAMACION_ACTUALIZAR", modulo = AuditModule.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#idProgramacion",
            detalle = "{nombre: #peticion.nombre, tipoRespaldo: #peticion.tipoRespaldo}")
    @Override
    @Transactional
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idProgramacion identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ScheduleResponse actualizar(Long idProgramacion, UpdateScheduleRequest peticion) {
        BackupSchedule programacion = obtenerOFallar(idProgramacion);

        programacion.setNombre(peticion.getNombre());
        programacion.setTipoRespaldo(peticion.getTipoRespaldo());
        programacion.setExpresionCron(peticion.getExpresionCron());
        programacion.setRetencionDias(peticion.getRetencionDias());
        programacion.setProximaEjecucion(calcularProximaEjecucion(peticion.getExpresionCron()));
        programacion.setActualizadoEn(LocalDateTime.now());

        return aRespuesta(programacionRepository.save(programacion));
    }

    @Auditable(accion = "RESPALDO_PROGRAMACION_CAMBIAR_ESTADO", modulo = AuditModule.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#idProgramacion", detalle = "{activo: #activo}")
    @Override
    @Transactional
    /**
     * Aplica una transicion de estado especifica sobre el ciclo de vida del recurso.
     *
     * @param idProgramacion identificador unico que referencia de manera univoca al registro
     * @param activo parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ScheduleResponse cambiarEstado(Long idProgramacion, boolean activo) {
        BackupSchedule programacion = obtenerOFallar(idProgramacion);
        programacion.setActivo(activo);
        programacion.setActualizadoEn(LocalDateTime.now());
        return aRespuesta(programacionRepository.save(programacion));
    }

    @Auditable(accion = "RESPALDO_PROGRAMACION_ELIMINAR", modulo = AuditModule.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#idProgramacion")
    @Override
    @Transactional
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idProgramacion identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminar(Long idProgramacion) {
        BackupSchedule programacion = obtenerOFallar(idProgramacion);
        programacionRepository.delete(programacion);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<ScheduleResponse> listar() {
        return programacionRepository.findAll().stream().map(this::aRespuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idProgramacion identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ScheduleResponse obtenerPorId(Long idProgramacion) {
        return aRespuesta(obtenerOFallar(idProgramacion));
    }

    private LocalDateTime calcularProximaEjecucion(String expresionCron) {
        try {
            LocalDateTime siguiente = CronExpression.parse(expresionCron).next(LocalDateTime.now());
            if (siguiente == null) {
                throw new BusinessRuleException("La expresión cron no produce ninguna ejecución futura.");
            }
            return siguiente;
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Expresión cron inválida: " + e.getMessage());
        }
    }

    private BackupSchedule obtenerOFallar(Long idProgramacion) {
        return programacionRepository.findById(idProgramacion)
                .orElseThrow(() -> new ResourceNotFoundException("Programación de respaldo no encontrada con ID: " + idProgramacion));
    }

    private ScheduleResponse aRespuesta(BackupSchedule programacion) {
        return ScheduleResponse.builder()
                .idProgramacion(programacion.getIdProgramacion())
                .nombre(programacion.getNombre())
                .tipoRespaldo(programacion.getTipoRespaldo())
                .expresionCron(programacion.getExpresionCron())
                .retencionDias(programacion.getRetencionDias())
                .activo(programacion.getActivo())
                .proximaEjecucion(programacion.getProximaEjecucion())
                .ultimaEjecucion(programacion.getUltimaEjecucion())
                .creadoPor(programacion.getCreadoPor())
                .fechaCreacion(programacion.getFechaCreacion())
                .build();
    }
}
