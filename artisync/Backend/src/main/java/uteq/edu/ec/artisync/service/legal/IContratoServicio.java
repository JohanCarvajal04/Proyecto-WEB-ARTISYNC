package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEstadoFirma;

public interface IContratoServicio {

    RespuestaContrato generarContrato(Long idPedido);

    RespuestaContrato firmarContrato(Long idContrato, Long idUsuario);

    RespuestaContrato obtenerContrato(Long idContrato);

    RespuestaContrato obtenerContratoPorPedido(Long idPedido);

    RespuestaEstadoFirma obtenerEstadoFirma(Long idContrato);

    byte[] generarPdf(Long idContrato);
}
