package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;

import java.util.List;

public interface SeguidorService {

    RespuestaSeguidor seguirCreador(Long idUsuario, Long idPerfilCreador);

    RespuestaMensaje dejarDeSeguir(Long idUsuario, Long idPerfilCreador);

    long contarSeguidores(Long idPerfilCreador);

    boolean estaSiguiendo(Long idUsuario, Long idPerfilCreador);

    List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador);
}
