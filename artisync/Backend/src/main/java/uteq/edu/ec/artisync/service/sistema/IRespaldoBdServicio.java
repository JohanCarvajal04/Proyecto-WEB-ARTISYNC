package uteq.edu.ec.artisync.service.sistema;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.sistema.PeticionActualizarPoliticaRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.sistema.RespuestaRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.sistema.ResumenRespaldos;
import uteq.edu.ec.artisync.entity.sistema.RespaldoBd;
import uteq.edu.ec.artisync.entity.sistema.RespaldoPolitica;
import uteq.edu.ec.artisync.util.PagedResponse;

public interface IRespaldoBdServicio {

    PagedResponse<RespuestaRespaldo> listar(Pageable pageable, String tipo, String categoria);

    RespuestaRespaldo crearRespaldoManual(String correoUsuario, RespaldoBd.CategoriaRespaldo categoria);

    RespuestaRespaldo importarRespaldoExterno(String correoUsuario, MultipartFile archivo);

    RespuestaRespaldo obtenerPorId(Long id);

    Resource descargar(Long id);

    void eliminar(Long id);

    void restaurar(Long id);

    ResumenRespaldos obtenerResumen();

    void ejecutarRespaldoAutomatico(RespaldoBd.CategoriaRespaldo categoria);

    void purgarExpirados();

    RespaldoPolitica obtenerPolitica();

    RespaldoPolitica actualizarPolitica(PeticionActualizarPoliticaRespaldo peticion);
}
