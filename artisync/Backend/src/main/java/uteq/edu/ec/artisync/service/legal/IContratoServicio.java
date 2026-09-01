package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEstadoFirma;

public interface IContratoServicio {

    RespuestaContrato generarContrato(Long idPedido, Long idUsuarioSolicitante);

    RespuestaContrato firmarContrato(Long idContrato, Long idUsuario);

    RespuestaContrato obtenerContrato(Long idContrato, Long idUsuarioSolicitante);

    RespuestaContrato obtenerContratoPorPedido(Long idPedido, Long idUsuarioSolicitante);

    RespuestaEstadoFirma obtenerEstadoFirma(Long idContrato, Long idUsuarioSolicitante);

    byte[] generarPdf(Long idContrato, Long idUsuarioSolicitante);
}
