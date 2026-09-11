package uteq.edu.ec.artisync.service.respaldo;

import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionActualizarProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearProgramacion;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaProgramacion;

import java.util.List;

/**
 * Contrato de Servicio (Interface) para la configuración de respaldos recurrentes (CRON).
 * 
 * Propósito: Gestionar las reglas de automatización (horarios, frecuencia, estado activo/inactivo) 
 * que rigen cuándo el sistema debe disparar un snapshot de base de datos sin intervención manual.
 * 
 * Responsabilidad arquitectónica: Encapsula la lógica de negocio de la agenda (scheduler) y 
 * provee un canal seguro para que los administradores alteren las políticas de backup desde el Frontend.
 */
public interface IRespaldoProgramacionServicio {

    RespuestaProgramacion crear(PeticionCrearProgramacion peticion, String creadoPor);

    RespuestaProgramacion actualizar(Long idProgramacion, PeticionActualizarProgramacion peticion);

    RespuestaProgramacion cambiarEstado(Long idProgramacion, boolean activo);

    void eliminar(Long idProgramacion);

    List<RespuestaProgramacion> listar();

    RespuestaProgramacion obtenerPorId(Long idProgramacion);
}
