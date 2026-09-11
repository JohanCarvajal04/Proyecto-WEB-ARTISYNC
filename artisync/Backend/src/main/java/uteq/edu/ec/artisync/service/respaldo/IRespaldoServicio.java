package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.util.PagedResponse;

/**
 * Contrato de Servicio (Interface) para la gestión del ciclo de vida de los backups.
 * 
 * Propósito: Proveer operaciones transaccionales para desencadenar respaldos manuales, 
 * restaurar la base de datos a partir de una instantánea (snapshot) y listar el historial 
 * de respaldos retenidos en el almacenamiento.
 * 
 * Responsabilidad arquitectónica: Actúa como la fachada de orquestación (Façade) 
 * que aísla los controladores de la capa de acceso a datos y scripts nativos de Docker/Postgres.
 */
public interface IRespaldoServicio {

    RespuestaRespaldo solicitarRespaldo(TipoRespaldo tipo, String correoSolicitante);

    PagedResponse<RespuestaRespaldo> listar(FiltroRespaldo filtro, Pageable pageable);

    RespuestaRespaldo obtenerPorId(Long idRespaldo);

    ArchivoRespaldo descargar(Long idRespaldo);

    void eliminar(Long idRespaldo);
}
