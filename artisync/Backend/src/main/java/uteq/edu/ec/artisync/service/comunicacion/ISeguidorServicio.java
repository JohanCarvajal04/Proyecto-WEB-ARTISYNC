package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaCreadorSeguidoNovedad;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;

import java.util.List;

public interface ISeguidorServicio {

    RespuestaEstadoSeguimiento seguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador);

    RespuestaEstadoSeguimiento dejarDeSeguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador);

    RespuestaEstadoSeguimiento obtenerEstadoSeguimiento(Long idUsuarioConsulta, Long idPerfilCreador);

    List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador);

    List<RespuestaCreadorSeguidoNovedad> listarCreadoresSeguidosNovedades(Long idUsuarioSeguidor);

    boolean actualizarPortadaYTitulo(Long idUsuario, String urlPortada, String tituloProfesional);
}
