package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.BackupResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.util.PagedResponse;

/**
 * Contract de Offering (Interface) para la gestión del ciclo de vida de los backups.
 * 
 * Propósito: Proveer operaciones transaccionales para desencadenar respaldos manuales, 
 * restaurar la base de datos a partir de una instantánea (snapshot) y listar el historial 
 * de respaldos retenidos en el almacenamiento.
 * 
 * Responsabilidad arquitectónica: Actúa como la fachada de orquestación (Façade) 
 * que aísla los controladores de la capa de acceso a datos y scripts nativos de Docker/Postgres.
 */
public interface IBackupService {

    BackupResponse solicitarRespaldo(BackupType tipo, String correoSolicitante);

    PagedResponse<BackupResponse> listar(BackupFilter filtro, Pageable pageable);

    BackupResponse obtenerPorId(Long idRespaldo);

    BackupFile descargar(Long idRespaldo);

    void eliminar(Long idRespaldo);
}
