package uteq.edu.ec.artisync.service.auditoria;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.audit.DatosEventoAuditoria;
import uteq.edu.ec.artisync.dto.peticion.auditoria.FiltroAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoriaResumen;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

public interface IAuditoriaServicio {

    /** REQUIRES_NEW: ver AuditoriaServicioImpl para el razonamiento completo. */
    void registrar(DatosEventoAuditoria datos);

    PagedResponse<RespuestaEventoAuditoriaResumen> listar(FiltroAuditoria filtro, Pageable pageable);

    RespuestaEventoAuditoria obtenerPorId(Long idEvento);

    /** Lanza ExcepcionReglaNegocio si el filtro devuelve más de 50 000 filas. */
    byte[] exportarCsv(FiltroAuditoria filtro);

    List<String> listarAccionesDisponibles();
}
