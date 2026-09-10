package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.util.PagedResponse;

public interface IRespaldoServicio {

    RespuestaRespaldo solicitarRespaldo(TipoRespaldo tipo, String correoSolicitante);

    PagedResponse<RespuestaRespaldo> listar(FiltroRespaldo filtro, Pageable pageable);

    RespuestaRespaldo obtenerPorId(Long idRespaldo);

    ArchivoRespaldo descargar(Long idRespaldo);

    void eliminar(Long idRespaldo);
}
