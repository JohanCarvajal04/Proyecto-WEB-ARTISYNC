package uteq.edu.ec.artisync.service.respaldo;

import uteq.edu.ec.artisync.dto.peticion.respaldo.UpdateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateScheduleRequest;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.ScheduleResponse;

import java.util.List;

/**
 * Contract de Offering (Interface) para la configuración de respaldos recurrentes (CRON).
 * 
 * Propósito: Gestionar las reglas de automatización (horarios, frecuencia, estado activo/inactivo) 
 * que rigen cuándo el sistema debe disparar un snapshot de base de datos sin intervención manual.
 * 
 * Responsabilidad arquitectónica: Encapsula la lógica de negocio de la agenda (scheduler) y 
 * provee un canal seguro para que los administradores alteren las políticas de backup desde el Frontend.
 */
public interface IBackupScheduleService {

    ScheduleResponse crear(CreateScheduleRequest peticion, String creadoPor);

    ScheduleResponse actualizar(Long idProgramacion, UpdateScheduleRequest peticion);

    ScheduleResponse cambiarEstado(Long idProgramacion, boolean activo);

    void eliminar(Long idProgramacion);

    List<ScheduleResponse> listar();

    ScheduleResponse obtenerPorId(Long idProgramacion);
}
