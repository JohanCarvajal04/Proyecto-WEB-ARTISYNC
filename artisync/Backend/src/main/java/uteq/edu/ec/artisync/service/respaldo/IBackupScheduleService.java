package uteq.edu.ec.artisync.service.respaldo;

import uteq.edu.ec.artisync.dto.peticion.respaldo.UpdateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateScheduleRequest;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.ScheduleResponse;

import java.util.List;

/**
 * Contrato de servicio para la configuración de respaldos recurrentes (CRON).
 * <p>
 * Propósito: gestionar las reglas de automatización (horarios, frecuencia, estado
 * activo/inactivo) que rigen cuándo el sistema debe disparar un snapshot de base de
 * datos sin intervención manual.
 * <p>
 * Responsabilidad arquitectónica: encapsula la lógica de negocio de la agenda (scheduler)
 * y provee un canal seguro para que los administradores alteren las políticas de backup
 * desde el frontend.
 */
public interface IBackupScheduleService {

    /**
     * Crea una nueva programación de respaldos recurrentes.
     *
     * @param peticion nombre, tipo de respaldo, expresión cron y días de retención
     * @param creadoPor correo de quien crea la programación
     * @return la programación creada, activa y con su próxima ejecución ya calculada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la expresión cron es inválida o no produce ninguna ejecución futura
     */
    ScheduleResponse create(CreateScheduleRequest peticion, String creadoPor);

    /**
     * Actualiza los datos de una programación existente y recalcula su próxima ejecución.
     *
     * @param idProgramacion identificador de la programación
     * @param peticion nuevos valores de nombre, tipo de respaldo, expresión cron y días de retención
     * @return la programación ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la expresión cron es inválida o no produce ninguna ejecución futura
     */
    ScheduleResponse update(Long idProgramacion, UpdateScheduleRequest peticion);

    /**
     * Activa o desactiva una programación, sin alterar el resto de sus datos.
     *
     * @param idProgramacion identificador de la programación
     * @param activo {@code true} para activarla, {@code false} para desactivarla
     * @return la programación con su estado ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación no existe
     */
    ScheduleResponse changeStatus(Long idProgramacion, boolean activo);

    /**
     * Elimina una programación de respaldos recurrentes.
     *
     * @param idProgramacion identificador de la programación a delete
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación no existe
     */
    void delete(Long idProgramacion);

    /**
     * Lista todas las programaciones de respaldo registradas.
     *
     * @return el listado completo de programaciones
     */
    List<ScheduleResponse> list();

    /**
     * Obtiene el detalle de una programación por su identificador.
     *
     * @param idProgramacion identificador de la programación
     * @return la programación solicitada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la programación no existe
     */
    ScheduleResponse getById(Long idProgramacion);
}
