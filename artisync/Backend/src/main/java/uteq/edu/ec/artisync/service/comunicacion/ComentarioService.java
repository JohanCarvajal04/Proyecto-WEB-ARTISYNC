package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;

public interface ComentarioService {

    RespuestaComentario agregarComentario(Long idUsuario, Long idItemPortafolio, PeticionCrearComentario peticion);

    RespuestaMensaje eliminarComentario(Long idUsuario, Long idComentario);

    Page<RespuestaComentario> listarComentariosPorItem(Long idItemPortafolio, Pageable pageable);
}
