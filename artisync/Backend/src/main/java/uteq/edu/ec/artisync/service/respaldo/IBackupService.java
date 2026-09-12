package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.BackupResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.util.PagedResponse;

/**
 * Contrato de servicio para la gestión del ciclo de vida de los respaldos de base de datos.
 * <p>
 * Propósito: proveer operaciones transaccionales para desencadenar respaldos manuales,
 * list el historial retenido y ofrecer sus archivos para descarga o eliminación.
 * <p>
 * Responsabilidad arquitectónica: actúa como la fachada de orquestación que aísla los
 * controladores de la capa de acceso a datos y de los scripts nativos de Docker/Postgres.
 */
public interface IBackupService {

    /**
     * Dispara un respaldo manual del tipo indicado, si no hay ya uno en progreso.
     *
     * @param tipo tipo de respaldo a execute (completo o incremental)
     * @param correoSolicitante correo de quien solicita el respaldo, registrado en el evento
     * @return el respaldo recién iniciado, con su identificador y estado {@code EN_PROGRESO}
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya hay un respaldo en progreso
     */
    BackupResponse requestBackup(BackupType tipo, String correoSolicitante);

    /**
     * Lista el historial de respaldos que cumplen el filtro indicado.
     *
     * @param filtro criterios de búsqueda (tipo, estado, origen, rango de fechas)
     * @param pageable configuración de paginación y ordenamiento
     * @return la página de respaldos solicitada
     */
    PagedResponse<BackupResponse> list(BackupFilter filtro, Pageable pageable);

    /**
     * Obtiene el detalle de un respaldo por su identificador.
     *
     * @param idRespaldo identificador del respaldo
     * @return el respaldo solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el respaldo no existe
     */
    BackupResponse getById(Long idRespaldo);

    /**
     * Ofrece el archivo de un respaldo ya generado, listo para transmitirse en streaming.
     *
     * @param idRespaldo identificador del respaldo
     * @return el archivo del respaldo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el respaldo no existe, aún no tiene archivo generado o el archivo no existe en disco
     */
    BackupFile download(Long idRespaldo);

    /**
     * Elimina un respaldo y su archivo en disco, si existe.
     *
     * @param idRespaldo identificador del respaldo a delete
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el respaldo no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el respaldo está en progreso, o si existen respaldos incrementales que dependen de él
     */
    void delete(Long idRespaldo);
}
