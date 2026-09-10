package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionActualizarProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearProgramacion;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaProgramacion;
import uteq.edu.ec.artisync.entity.respaldo.RespaldoProgramacion;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoProgramacionRepository;
import uteq.edu.ec.artisync.service.respaldo.IRespaldoProgramacionServicio;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RespaldoProgramacionServicioImpl implements IRespaldoProgramacionServicio {

    private final RespaldoProgramacionRepository programacionRepository;

    @Auditable(accion = "RESPALDO_PROGRAMACION_CREAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#resultado.idProgramacion",
            detalle = "{nombre: #peticion.nombre, tipoRespaldo: #peticion.tipoRespaldo}")
    @Override
    @Transactional
    public RespuestaProgramacion crear(PeticionCrearProgramacion peticion, String creadoPor) {
        LocalDateTime proximaEjecucion = calcularProximaEjecucion(peticion.getExpresionCron());
        LocalDateTime ahora = LocalDateTime.now();

        RespaldoProgramacion programacion = RespaldoProgramacion.builder()
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

    @Auditable(accion = "RESPALDO_PROGRAMACION_ACTUALIZAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#idProgramacion",
            detalle = "{nombre: #peticion.nombre, tipoRespaldo: #peticion.tipoRespaldo}")
    @Override
    @Transactional
    public RespuestaProgramacion actualizar(Long idProgramacion, PeticionActualizarProgramacion peticion) {
        RespaldoProgramacion programacion = obtenerOFallar(idProgramacion);

        programacion.setNombre(peticion.getNombre());
        programacion.setTipoRespaldo(peticion.getTipoRespaldo());
        programacion.setExpresionCron(peticion.getExpresionCron());
        programacion.setRetencionDias(peticion.getRetencionDias());
        programacion.setProximaEjecucion(calcularProximaEjecucion(peticion.getExpresionCron()));
        programacion.setActualizadoEn(LocalDateTime.now());

        return aRespuesta(programacionRepository.save(programacion));
    }

    @Auditable(accion = "RESPALDO_PROGRAMACION_CAMBIAR_ESTADO", modulo = ModuloAuditoria.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#idProgramacion", detalle = "{activo: #activo}")
    @Override
    @Transactional
    public RespuestaProgramacion cambiarEstado(Long idProgramacion, boolean activo) {
        RespaldoProgramacion programacion = obtenerOFallar(idProgramacion);
        programacion.setActivo(activo);
        programacion.setActualizadoEn(LocalDateTime.now());
        return aRespuesta(programacionRepository.save(programacion));
    }

    @Auditable(accion = "RESPALDO_PROGRAMACION_ELIMINAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "respaldo_programaciones", idEntidad = "#idProgramacion")
    @Override
    @Transactional
    public void eliminar(Long idProgramacion) {
        RespaldoProgramacion programacion = obtenerOFallar(idProgramacion);
        programacionRepository.delete(programacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaProgramacion> listar() {
        return programacionRepository.findAll().stream().map(this::aRespuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaProgramacion obtenerPorId(Long idProgramacion) {
        return aRespuesta(obtenerOFallar(idProgramacion));
    }

    private LocalDateTime calcularProximaEjecucion(String expresionCron) {
        try {
            LocalDateTime siguiente = CronExpression.parse(expresionCron).next(LocalDateTime.now());
            if (siguiente == null) {
                throw new ExcepcionReglaNegocio("La expresión cron no produce ninguna ejecución futura.");
            }
            return siguiente;
        } catch (IllegalArgumentException e) {
            throw new ExcepcionReglaNegocio("Expresión cron inválida: " + e.getMessage());
        }
    }

    private RespaldoProgramacion obtenerOFallar(Long idProgramacion) {
        return programacionRepository.findById(idProgramacion)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Programación de respaldo no encontrada con ID: " + idProgramacion));
    }

    private RespuestaProgramacion aRespuesta(RespaldoProgramacion programacion) {
        return RespuestaProgramacion.builder()
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
